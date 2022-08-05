package org.workcraft.utils;

import org.workcraft.Framework;
import org.workcraft.dom.Model;
import org.workcraft.dom.math.MathModel;
import org.workcraft.dom.visual.VisualModel;
import org.workcraft.exceptions.ModelValidationException;
import org.workcraft.exceptions.NoExporterException;
import org.workcraft.exceptions.SerialisationException;
import org.workcraft.interop.Exporter;
import org.workcraft.interop.Format;
import org.workcraft.plugins.PluginManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public class ExportUtils {

    public static Exporter chooseBestExporter(Model model, Format format) {
        return chooseBestExporter(model, format.getName(), format.getUuid());
    }

    public static Exporter chooseBestExporter(Model model, String formatName, UUID formatUuid) {
        final PluginManager pm = Framework.getInstance().getPluginManager();
        for (Exporter exporter : pm.getSortedExporters()) {
            if (exporter.isCompatible(model)) {
                Format format = exporter.getFormat();
                boolean formatMatchByName = (formatName != null) && formatName.equalsIgnoreCase(format.getName());
                boolean formatMatchByUuid = (formatUuid != null) && formatUuid.equals(format.getUuid());
                if (formatMatchByName || formatMatchByUuid) {
                    return exporter;
                }
            }
        }
        return null;
    }

    public static void exportToFile(Model model, File file, Format format)
            throws IOException, ModelValidationException, SerialisationException {

        Exporter exporter = chooseBestExporter(model, format);
        if (exporter == null) {
            throw new NoExporterException(model.getDisplayName(), format.getName());
        }
        exportToFile(exporter, model, file);
    }

    public static void exportToFile(Exporter exporter, Model model, File file)
            throws IOException, ModelValidationException, SerialisationException {

        file.createNewFile();
        FileOutputStream fos = new FileOutputStream(file);
        boolean success = false;
        try {
            // For incompatible visual model try exporting its underlying math model.
            if ((model instanceof VisualModel) && !exporter.isCompatible(model)) {
                MathModel mathModel = ((VisualModel) model).getMathModel();
                if (exporter.isCompatible(mathModel)) {
                    model = mathModel;
                } else {
                    String exporterName = exporter.getFormat().getDescription();
                    String modelName = model.getDisplayName();
                    String text = "Exporter to " + exporterName + " is not compatible with " + modelName + " model.";
                    throw new RuntimeException(text);
                }
            }
            exporter.export(model, fos);
            success = true;
        } finally {
            fos.close();
            if (!success) {
                file.delete();
            }
        }
    }

    public static String asIdentifier(String title) {
        // Non-empty module name is required.
        if ((title == null) || title.isEmpty()) {
            title = "Untitled";
        }
        // If the title start with a number, then prepend it with an underscore.
        if (Character.isDigit(title.charAt(0))) {
            title = "_" + title;
        }
        // Replace spaces and special symbols with underscores.
        return title.replaceAll("[^A-Za-z0-9_]", "_");
    }

    public static String getExportMessage(Model model, File file) {
        return getExportMessage(model, file, "Exporting model");
    }

    public static String getExportMessage(Model model, File file, String description) {
        String result = description;
        String title = model.getTitle();
        if (!title.isEmpty()) {
            result += " '" + title + "'";
        }
        result += " to file '" + file.getAbsolutePath() + "'.";
        return result;
    }

}
