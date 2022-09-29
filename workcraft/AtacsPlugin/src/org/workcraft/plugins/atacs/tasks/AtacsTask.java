package org.workcraft.plugins.atacs.tasks;

import org.workcraft.Framework;
import org.workcraft.exceptions.NoExporterException;
import org.workcraft.interop.Exporter;
import org.workcraft.interop.ExternalProcessListener;
import org.workcraft.plugins.atacs.AtacsSettings;
import org.workcraft.plugins.circuit.utils.VerilogUtils;
import org.workcraft.plugins.circuit.verilog.VerilogModule;
import org.workcraft.plugins.stg.Mutex;
import org.workcraft.plugins.stg.Stg;
import org.workcraft.plugins.stg.interop.LpnFormat;
import org.workcraft.plugins.stg.utils.MutexUtils;
import org.workcraft.plugins.stg.utils.StgUtils;
import org.workcraft.tasks.*;
import org.workcraft.utils.*;
import org.workcraft.workspace.WorkspaceEntry;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AtacsTask implements Task<AtacsOutput>, ExternalProcessListener {
    private final WorkspaceEntry we;
    private final List<String> args;
    private final Collection<Mutex> mutexes;

    public AtacsTask(WorkspaceEntry we, List<String> args, Collection<Mutex> mutexes) {
        this.we = we;
        this.args = args;
        this.mutexes = mutexes;
    }

    @Override
    public Result<? extends AtacsOutput> run(ProgressMonitor<? super AtacsOutput> monitor) {
        ArrayList<String> command = new ArrayList<>();

        // Name of the executable
        String toolName = ExecutableUtils.getAbsoluteCommandPath(AtacsSettings.getCommand());
        command.add(toolName);

        // Built-in arguments
        command.addAll(args);

        // Extra arguments (should go before the file parameters)
        String extraArgs = AtacsSettings.getArgs();
        if (AtacsSettings.getAdvancedMode()) {
            String tmp = DialogUtils.showInput("Additional parameters for ATACS:", extraArgs);
            if (tmp == null) {
                return Result.cancel();
            }
            extraArgs = tmp;
        }
        command.addAll(TextUtils.splitWords(extraArgs));

        command.add("-oq"); // Quiet mode
        command.add("-ll"); // Load LPN as Petri net (instead of Timed Event/Rule Structure, which is default semantics)
        command.add("-ys"); // Do circuit synthesis
        command.add("-sV"); // Output synthesis result in Verilog format

        String prefix = FileUtils.getTempPrefix(we.getTitle());
        File directory = FileUtils.createTempDirectory(prefix);
        Stg stg = WorkspaceUtils.getAs(we, Stg.class);

        // Input file
        File stgFile = getInputFile(stg, directory);
        command.add(stgFile.getAbsolutePath());
        String verilogFileName = stgFile.getName().replace(".lpn", ".v");
        File verilogFile = new File(directory, verilogFileName);

        boolean printStdout = AtacsSettings.getPrintStdout();
        boolean printStderr = AtacsSettings.getPrintStderr();
        ExternalProcessTask task = new ExternalProcessTask(command, directory, printStdout, printStderr);
        SubtaskMonitor<ExternalProcessOutput> subtaskMonitor = new SubtaskMonitor<>(monitor);
        Result<? extends ExternalProcessOutput> result = task.run(subtaskMonitor);
        FileUtils.deleteOnExitRecursively(directory);

        ExternalProcessOutput output = result.getPayload();
        if ((result.isSuccess()) && (output != null)) {
            if (output.getReturnCode() != 0) {
                return Result.failure(new AtacsOutput(output, null));
            }
            VerilogModule verilogModule = VerilogUtils.importTopVerilogModule(verilogFile);
            return Result.success(new AtacsOutput(output, verilogModule));
        }

        if (result.isCancel()) {
            return Result.cancel();
        }

        return Result.exception(result.getCause());
    }

    private File getInputFile(Stg stg, File directory) {
        LpnFormat format = LpnFormat.getInstance();
        Exporter exporter = ExportUtils.chooseBestExporter(stg, format);
        if (exporter == null) {
            throw new NoExporterException(stg, format);
        }

        TaskManager taskManager = Framework.getInstance().getTaskManager();
        String extension = format.getExtension();
        File file = new File(directory, StgUtils.SPEC_FILE_PREFIX + extension);
        ExportTask exportTask = new ExportTask(exporter, stg, file);
        Result<? extends ExportOutput> exportResult = taskManager.execute(exportTask, "Exporting .lpn");
        if (!exportResult.isSuccess()) {
            throw new RuntimeException("Unable to export the model.");
        }
        if (!mutexes.isEmpty()) {
            stg = StgUtils.loadOrImportStg(file);
            MutexUtils.factoroutMutexes(stg, mutexes);
            file = new File(directory, StgUtils.SPEC_FILE_PREFIX + StgUtils.MUTEX_FILE_SUFFIX + extension);
            exportTask = new ExportTask(exporter, stg, file);
            exportResult = taskManager.execute(exportTask, "Exporting .lpn");
            if (!exportResult.isSuccess()) {
                throw new RuntimeException("Unable to export the model after factoring out the mutexes.");
            }
        }
        return file;
    }

    @Override
    public void processFinished(int returnCode) {
    }

    @Override
    public void errorData(byte[] data) {
    }

    @Override
    public void outputData(byte[] data) {
    }

}
