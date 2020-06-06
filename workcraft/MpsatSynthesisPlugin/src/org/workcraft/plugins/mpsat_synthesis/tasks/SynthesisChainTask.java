package org.workcraft.plugins.mpsat_synthesis.tasks;

import org.workcraft.Framework;
import org.workcraft.exceptions.NoExporterException;
import org.workcraft.interop.Exporter;
import org.workcraft.plugins.mpsat_synthesis.SynthesisMode;
import org.workcraft.plugins.punf.PunfSettings;
import org.workcraft.plugins.punf.tasks.PunfOutput;
import org.workcraft.plugins.punf.tasks.PunfTask;
import org.workcraft.plugins.stg.Mutex;
import org.workcraft.plugins.stg.Stg;
import org.workcraft.plugins.stg.interop.StgFormat;
import org.workcraft.plugins.stg.utils.MutexUtils;
import org.workcraft.plugins.stg.utils.StgUtils;
import org.workcraft.tasks.*;
import org.workcraft.utils.ExportUtils;
import org.workcraft.utils.FileUtils;
import org.workcraft.utils.WorkspaceUtils;
import org.workcraft.workspace.WorkspaceEntry;

import java.io.File;
import java.util.Collection;

public class SynthesisChainTask implements Task<SynthesisChainOutput> {
    private final WorkspaceEntry we;
    private final SynthesisMode synthesisMode;
    private final Collection<Mutex> mutexes;

    public SynthesisChainTask(WorkspaceEntry we, SynthesisMode synthesisMode, Collection<Mutex> mutexes) {
        this.we = we;
        this.synthesisMode = synthesisMode;
        this.mutexes = mutexes;
    }

    @Override
    public Result<? extends SynthesisChainOutput> run(ProgressMonitor<? super SynthesisChainOutput> monitor) {
        TaskManager taskManager = Framework.getInstance().getTaskManager();
        String prefix = FileUtils.getTempPrefix(we.getTitle());
        File directory = FileUtils.createTempDirectory(prefix);
        StgFormat format = StgFormat.getInstance();
        String stgFileExtension = format.getExtension();
        try {
            Stg model = WorkspaceUtils.getAs(we, Stg.class);
            Exporter exporter = ExportUtils.chooseBestExporter(model, format);
            if (exporter == null) {
                throw new NoExporterException(model, format);
            }
            SubtaskMonitor<Object> subtaskMonitor = new SubtaskMonitor<>(monitor);

            // Generate .g for the model
            String filePrefix = StgUtils.SPEC_FILE_PREFIX;
            File netFile = new File(directory, filePrefix + stgFileExtension);
            ExportTask exportTask = new ExportTask(exporter, model, netFile);
            Result<? extends ExportOutput> exportResult = taskManager.execute(
                    exportTask, "Exporting .g", subtaskMonitor);

            if (!exportResult.isSuccess()) {
                if (exportResult.isCancel()) {
                    return Result.cancel();
                }
                return Result.failure(new SynthesisChainOutput(exportResult, null, null, synthesisMode));
            }
            if ((mutexes != null) && !mutexes.isEmpty()) {
                model = StgUtils.loadStg(netFile);
                MutexUtils.factoroutMutexs(model, mutexes);
                filePrefix += StgUtils.MUTEX_FILE_SUFFIX;
                netFile = new File(directory, filePrefix + stgFileExtension);
                exportTask = new ExportTask(exporter, model, netFile);
                exportResult = taskManager.execute(exportTask, "Exporting .g");

                if (!exportResult.isSuccess()) {
                    if (exportResult.isCancel()) {
                        return Result.cancel();
                    }
                    return Result.failure(new SynthesisChainOutput(exportResult, null, null, synthesisMode));
                }
            }
            monitor.progressUpdate(0.33);

            // Generate unfolding
            boolean useLegacyMci = PunfSettings.getUseMciCsc() && (synthesisMode == SynthesisMode.RESOLVE_ENCODING_CONFLICTS);
            String unfoldingExtension = useLegacyMci ? PunfTask.MCI_FILE_EXTENSION : PunfTask.PNML_FILE_EXTENSION;
            File unfoldingFile = new File(directory, filePrefix + unfoldingExtension);
            PunfTask punfTask = new PunfTask(netFile, unfoldingFile, directory);
            Result<? extends PunfOutput> punfResult = taskManager.execute(punfTask, "Unfolding .g", subtaskMonitor);

            if (!punfResult.isSuccess()) {
                if (punfResult.isCancel()) {
                    return Result.cancel();
                }
                return Result.failure(new SynthesisChainOutput(
                        exportResult, punfResult, null, synthesisMode));
            }
            monitor.progressUpdate(0.66);

            // Run MPSat on the generated unfolding
            MpsatTask mpsatTask = new MpsatTask(unfoldingFile, synthesisMode, directory);
            Result<? extends MpsatOutput> mpsatResult = taskManager.execute(
                    mpsatTask, "Running synthesis [MPSat]", subtaskMonitor);

            if (!mpsatResult.isSuccess()) {
                if (mpsatResult.isCancel()) {
                    return Result.cancel();
                }
                return Result.failure(new SynthesisChainOutput(exportResult, punfResult, mpsatResult, synthesisMode));
            }
            monitor.progressUpdate(1.0);

            return Result.success(new SynthesisChainOutput(exportResult, punfResult, mpsatResult, synthesisMode));
        } catch (Throwable e) {
            return new Result<>(e);
        } finally {
            FileUtils.deleteOnExitRecursively(directory);
        }
    }

}
