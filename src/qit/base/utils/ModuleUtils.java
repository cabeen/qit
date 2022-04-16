/*******************************************************************************
 *
 * Quantitative Imaging Toolkit (QIT) (c) 2012-2022 Ryan Cabeen
 * All rights reserved.
 *
 * The Software remains the property of Ryan Cabeen ("the Author").
 *
 * The Software is distributed "AS IS" under this Licence solely for
 * non-commercial use in the hope that it will be useful, but in order
 * that the Author as a charitable foundation protects its assets for
 * the benefit of its educational and research purposes, the Author
 * makes clear that no condition is made or to be implied, nor is any
 * warranty given or to be implied, as to the accuracy of the Software,
 * or that it will be suitable for any particular purpose or for use
 * under any specific conditions. Furthermore, the Author disclaims
 * all responsibility for the use which is made of the Software. It
 * further disclaims any liability for the outcomes arising from using
 * the Software.
 *
 * The Licensee agrees to indemnify the Author and hold the
 * Author harmless from and against any and all claims, damages and
 * liabilities asserted by third parties (including claims for
 * negligence) which arise directly or indirectly from the use of the
 * Software or the sale of any products based on the Software.
 *
 * No part of the Software may be reproduced, modified, transmitted or
 * transferred in any form or by any means, electronic or mechanical,
 * without the express permission of the Author. The permission of
 * the Author is not required if the said reproduction, modification,
 * transmission or transference is done without financial return, the
 * conditions of this Licence are imposed upon the receiver of the
 * product, and all original and amended source code is included in any
 * transmitted product. You may be held legally responsible for any
 * copyright infringement that is caused or encouraged by your failure to
 * abide by these terms and conditions.
 *
 * You are not permitted under this Licence to use this Software
 * commercially. Use for which any financial return is received shall be
 * defined as commercial use, and includes (1) integration of all or part
 * of the source code or the Software into a product for sale or license
 * by or on behalf of Licensee to third parties or (2) use of the
 * Software or any derivative of it for research with the final aim of
 * developing software products for sale or license to a third party or
 * (3) use of the Software or any derivative of it for research with the
 * final aim of developing non-software products for sale or license to a
 * third party, or (4) use of the Software to provide any service to an
 * external organisation for which payment is received.
 *
 ******************************************************************************/


package qit.base.utils;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.reflections.Reflections;
import qit.base.Dataset;
import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.data.datasets.Affine;
import qit.data.datasets.Curves;
import qit.data.datasets.Deformation;
import qit.data.datasets.Mask;
import qit.data.datasets.Matrix;
import qit.data.datasets.Mesh;
import qit.data.datasets.Neuron;
import qit.data.datasets.Solids;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.utils.mri.structs.Gradients;

/**
 * utilities for manipulating file paths
 */
public class ModuleUtils
{
    public static final String ICON_DATA = "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&#xA;xpt47TNLUwAAAABJRU5ErkJggg==";

    public static void writePipelines(String directory)
    {
        try
        {
            PathUtils.mkdirs(directory);

            for (Module module : ModuleUtils.list())
            {
                Class<? extends Module> type = module.getClass();
                String moduleName = type.getSimpleName();
                writePipeline(module, PathUtils.join(directory, moduleName + ".pipe"));
            }
        }
        catch (IOException io)
        {
            Logging.error("failed to write pipelines: " + io.getMessage());
        }
    }

    public static void writePipelines(String server, String path, String directory)
    {
        try
        {
            PathUtils.mkdirs(directory);

            for (Module module : ModuleUtils.list())
            {
                Class<? extends Module> type = module.getClass();
                String moduleName = type.getSimpleName();
                writePipeline(server, path, module, PathUtils.join(directory, moduleName + ".pipe"));
            }
        }
        catch (IOException io)
        {
            Logging.error("failed to write pipelines: " + io.getMessage());
        }
    }

    public static void writePipeline(Module module, String fn)
    {
        writePipeline(Global.PIPELINE_HOST, Global.PIPELINE_PATH, module, fn);
    }

    public static void writePipeline(String server, String path, Module module, String fn)
    {
        try
        {
            Class<? extends Module> type = module.getClass();

            ModuleDescription moduleAnnot = type.getAnnotation(ModuleDescription.class);
            String moduleDescription = (moduleAnnot != null) ? moduleAnnot.value() : "no description provided";
            String moduleName = type.getSimpleName();

            String date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());

            Element pipelineElement = new Element("pipeline");
            pipelineElement.setAttribute(new Attribute("version", ".2"));

            {
                Element iconsElement = new Element("icons");
                pipelineElement.addContent(iconsElement);

                {
                    Element iconElement = new Element("icon");
                    iconElement.setAttribute(new Attribute("id", "0"));
                    iconElement.setAttribute(new Attribute("value", ICON_DATA));
                    iconsElement.addContent(iconElement);
                }
            }

            {
                Element moduleGroupElement = new Element("moduleGroup");
                moduleGroupElement.setAttribute(new Attribute("id", "myid"));
                moduleGroupElement.setAttribute(new Attribute("name", "myname"));
                moduleGroupElement.setAttribute(new Attribute("posX", "256"));
                moduleGroupElement.setAttribute(new Attribute("posY", "256"));
                moduleGroupElement.setAttribute(new Attribute("sourceCode", ""));
                pipelineElement.addContent(moduleGroupElement);

                {
                    Element moduleElement = new Element("module");
                    moduleElement.setAttribute(new Attribute("id", "myid"));
                    moduleElement.setAttribute(new Attribute("name", moduleName));
                    moduleElement.setAttribute(new Attribute("location", String.format("pipeline://%s/%s", server, path)));
                    moduleElement.setAttribute(new Attribute("package", "Quantitative Imaging Toolkit"));
                    moduleElement.setAttribute(new Attribute("description", moduleDescription));
                    moduleElement.setAttribute(new Attribute("icon", "0"));
                    moduleElement.setAttribute(new Attribute("posX", "256"));
                    moduleElement.setAttribute(new Attribute("posY", "256"));
                    moduleElement.setAttribute(new Attribute("sourceCode", ""));
                    moduleElement.setAttribute(new Attribute("version", "1.0b"));
                    moduleElement.setAttribute(new Attribute("executableVersion", "1.0b"));
                    moduleGroupElement.addContent(moduleElement);

                    {
                        Element urlElement = new Element("url");
                        urlElement.addContent("http://brayns.com/qitwiki");
                        moduleElement.addContent(urlElement);
                    }

                    {
                        ModuleCitation citation = type.getAnnotation(ModuleCitation.class);
                        if (citation != null)
                        {
                            {
                                Element citationsElement = new Element("citations");
                                moduleElement.addContent(citationsElement);

                                {
                                    String text = citation.value().replace("&", "and");

                                    Element citationElement = new Element("citation");
                                    citationElement.addContent(text);
                                    citationsElement.addContent(citationElement);
                                }
                            }
                        }
                    }

                    // pipeline author
                    {
                        Element authorsElement = new Element("authors");
                        moduleElement.addContent(authorsElement);

                        {
                            Element authorElement = new Element("author");
                            authorElement.setAttribute(new Attribute("fullName", "Ryan Cabeen"));
                            authorElement.setAttribute(new Attribute("email", "cabeen@gmail.com"));
                            authorElement.setAttribute(new Attribute("website", "http://cabeen.io"));
                            authorsElement.addContent(authorElement);
                        }
                    }

                    // executable author
                    {
                        Element authorsElement = new Element("executableAuthors");
                        moduleElement.addContent(authorsElement);

                        {
                            ModuleAuthor author = type.getAnnotation(ModuleAuthor.class);
                            if (author != null)
                            {
                                String authorName = author.value();

                                if (!authorName.equals("Ryan Cabeen"))
                                {
                                    Element authorElement = new Element("author");
                                    authorElement.setAttribute(new Attribute("fullName", authorName));
                                    authorsElement.addContent(authorElement);
                                }
                            }
                        }

                        Element authorElement = new Element("author");
                        authorElement.setAttribute(new Attribute("fullName", "Ryan Cabeen"));
                        authorElement.setAttribute(new Attribute("email", "cabeen@gmail.com"));
                        authorElement.setAttribute(new Attribute("website", "http://cabeen.io"));
                        authorsElement.addContent(authorElement);
                    }

                    // license
                    {
                        Element licenseElement = new Element("license");
                        licenseElement.addContent(Global.getLicense());
                        moduleElement.addContent(licenseElement);
                    }

                    // metadata
                    {
                        Element metadataElement = new Element("metadata");
                        moduleElement.addContent(metadataElement);

                        {
                            Element dataElement = new Element("data");
                            dataElement.setAttribute(new Attribute("key", "__createDateKey"));
                            dataElement.setAttribute(new Attribute("value", date));
                            metadataElement.addContent(dataElement);
                        }
                    }

                    int count = 0;

                    // the module name
                    {
                        Element inputElement = new Element("input");
                        inputElement.setAttribute(new Attribute("id", "_1." + "ModuleName"));
                        inputElement.setAttribute(new Attribute("name", "ModuleName"));
                        inputElement.setAttribute(new Attribute("description", "specify the module name"));
                        inputElement.setAttribute(new Attribute("required", "true"));
                        inputElement.setAttribute(new Attribute("enabled", "true"));
                        inputElement.setAttribute(new Attribute("order", String.valueOf(count)));
                        moduleElement.addContent(inputElement);

                        {
                            Element formatElement = new Element("format");
                            formatElement.setAttribute(new Attribute("type", "String"));
                            formatElement.setAttribute(new Attribute("cardinality", "1"));
                            inputElement.addContent(formatElement);
                        }

                        {
                            Element valuesElement = new Element("values");
                            inputElement.addContent(valuesElement);

                            {
                                Element valueElement = new Element("value");
                                valueElement.addContent(moduleName);
                                valuesElement.addContent(valueElement);
                            }
                        }

                        count += 1;
                    }

                    // the verbose flag
                    {
                        Element inputElement = new Element("input");
                        inputElement.setAttribute(new Attribute("id", "_1." + "VerboseFlag"));
                        inputElement.setAttribute(new Attribute("name", "VerboseFlag"));
                        inputElement.setAttribute(new Attribute("description", "enable verbose messaging"));
                        inputElement.setAttribute(new Attribute("required", "true"));
                        inputElement.setAttribute(new Attribute("enabled", "true"));
                        inputElement.setAttribute(new Attribute("order", String.valueOf(count)));
                        moduleElement.addContent(inputElement);

                        {
                            Element formatElement = new Element("format");
                            formatElement.setAttribute(new Attribute("type", "String"));
                            formatElement.setAttribute(new Attribute("cardinality", "1"));
                            inputElement.addContent(formatElement);
                        }

                        {
                            Element valuesElement = new Element("values");
                            inputElement.addContent(valuesElement);

                            {
                                Element valueElement = new Element("value");
                                valueElement.addContent("--verbose");
                                valuesElement.addContent(valueElement);
                            }
                        }

                        count += 1;
                    }

                    // the debugging flag
                    {
                        Element inputElement = new Element("input");
                        inputElement.setAttribute(new Attribute("id", "_1." + "DebugFlag"));
                        inputElement.setAttribute(new Attribute("name", "DebugFlag"));
                        inputElement.setAttribute(new Attribute("description", "enable debugging mode"));
                        inputElement.setAttribute(new Attribute("required", "true"));
                        inputElement.setAttribute(new Attribute("enabled", "true"));
                        inputElement.setAttribute(new Attribute("order", String.valueOf(count)));
                        moduleElement.addContent(inputElement);

                        {
                            Element formatElement = new Element("format");
                            formatElement.setAttribute(new Attribute("type", "String"));
                            formatElement.setAttribute(new Attribute("cardinality", "1"));
                            inputElement.addContent(formatElement);
                        }

                        {
                            Element valuesElement = new Element("values");
                            inputElement.addContent(valuesElement);

                            {
                                Element valueElement = new Element("value");
                                valueElement.addContent("--debug");
                                valuesElement.addContent(valueElement);
                            }
                        }

                        count += 1;
                    }

                    for (Field field : ModuleUtils.fields(module))
                    {
                        boolean optional = field.getAnnotation(ModuleOptional.class) != null;
                        boolean advanced = field.getAnnotation(ModuleAdvanced.class) != null;
                        boolean input = field.getAnnotation(ModuleInput.class) != null;
                        boolean output = field.getAnnotation(ModuleOutput.class) != null;
                        boolean binary = field.getType().equals(boolean.class) || field.getType().equals(Boolean.class);
                        boolean string = field.getType().equals(String.class);

                        ModuleDescription fieldAnnot = field.getAnnotation(ModuleDescription.class);
                        String fieldDescription = (fieldAnnot != null) ? fieldAnnot.value() : "no description provided";
                        String fieldName = field.getName();
                        Object fieldValue = ModuleUtils.value(module, field);
                        boolean fieldDefined = fieldValue != null && !binary;

                        if (fieldName.equals("inplace"))
                        {
                            continue;
                        }

                        if (advanced)
                        {
                            fieldDescription = fieldDescription + " [Advanced]";
                        }

                        String fieldType = field.getType().getSimpleName();
                        fieldDescription = String.format("%s (Datatype: %s)", fieldDescription, fieldType);

                        Element inputElement = new Element(output ? "output" : "input");
                        inputElement.setAttribute(new Attribute("id", "_1." + fieldName));
                        inputElement.setAttribute(new Attribute("name", fieldName));
                        inputElement.setAttribute(new Attribute("description", fieldDescription));
                        inputElement.setAttribute(new Attribute("required", optional || binary || fieldDefined ? "false" : "true"));
                        inputElement.setAttribute(new Attribute("enabled", binary || optional || fieldDefined ? "false" : "true"));
                        inputElement.setAttribute(new Attribute("prefix", "--" + fieldName));
                        inputElement.setAttribute(new Attribute("prefixSpaced", "true"));
                        inputElement.setAttribute(new Attribute("order", String.valueOf(count)));
                        moduleElement.addContent(inputElement);

                        if (input || output)
                        {
                            Element formatElement = new Element("format");
                            formatElement.setAttribute(new Attribute("type", "File"));
                            formatElement.setAttribute(new Attribute("cardinality", "1"));
                            inputElement.addContent(formatElement);

                            {
                                // @TODO: specify the file extension, e.g. nii.gz, vtk, etc.
                                Element filetypeElement = new Element("filetype");
                                filetypeElement.setAttribute(new Attribute("name", "File"));
                                filetypeElement.setAttribute(new Attribute("description", "Any type of data file"));
                                inputElement.addContent(filetypeElement);
                            }
                        }
                        else
                        {
                            Element formatElement = new Element("format");
                            formatElement.setAttribute(new Attribute("type", string ? "String" : "Number"));
                            formatElement.setAttribute(new Attribute("cardinality", binary ? "0" : "1"));
                            inputElement.addContent(formatElement);
                        }


                        if (fieldDefined)
                        {
                            Element valuesElement = new Element("values");
                            inputElement.addContent(valuesElement);

                            {
                                Element valueElement = new Element("value");
                                valueElement.addContent(fieldValue.toString());
                                valuesElement.addContent(valueElement);
                            }
                        }

                        count += 1;
                    }
                }
            }

            Document document = new Document(pipelineElement);

            // new XMLOutputter().output(doc, System.out);
            XMLOutputter xmlOutput = new XMLOutputter();

            // display nice nice
            xmlOutput.setFormat(Format.getPrettyFormat());
            String xml = xmlOutput.outputString(document);

            // note: pipeline expects an ampersand in the icon string
            // unfortunately, this is technically invalid xml, so jdom replaces it with '&amp;'...
            // let's do this to play nicely with pipeline
            xml = xml.replace("&amp;", "&");

            FileUtils.writeStringToFile(new File(fn), xml);
        }
        catch (IOException io)
        {
            Logging.error(io.getMessage());
        }
    }

    public static List<Module> list()
    {
        Reflections reflections = new Reflections("qit");

        List<Module> out = Lists.newArrayList();
        for (final Class<? extends Module> c : reflections.getSubTypesOf(Module.class))
        {
            if (c != null && c.getAnnotation(ModuleUnlisted.class) == null)
            {
                if (c.getName().contains("$"))
                {
                    continue;
                }

                try
                {
                    out.add(c.newInstance());
                }
                catch (Exception e)
                {
                    Logging.info("error, skipping: " + c.getName());
                    // skip any class that throws an exception
                }
            }
        }
        return out;
    }

    public static List<String> names()
    {
        final List<String> keys = Lists.newArrayList(listedClasses().keySet());
        Collections.sort(keys);
        return keys;
    }

    public static Map<String, Class<? extends Module>> listedClasses()
    {
        Reflections reflections = new Reflections("qit");
        final Map<String, Class<? extends Module>> listedModules = Maps.newHashMap();

        for (Class<? extends Module> clas : reflections.getSubTypesOf(Module.class))
        {
            String[] tokens = org.apache.commons.lang3.StringUtils.split(clas.getName(), ".");
            String name = tokens[tokens.length - 1];

            if (name.contains("$"))
            {
                continue;
            }

            if (clas.getAnnotation(ModuleUnlisted.class) == null)
            {
                listedModules.put(name, clas);
            }
        }

        return listedModules;
    }

    public static Map<String, Class<? extends Module>> unlistedClasses()
    {
        Reflections reflections = new Reflections("qit");
        final Map<String, Class<? extends Module>> unlistedModules = Maps.newHashMap();

        for (Class<? extends Module> clas : reflections.getSubTypesOf(Module.class))
        {
            String[] tokens = org.apache.commons.lang3.StringUtils.split(clas.getName(), ".");
            String name = tokens[tokens.length - 1];

            if (name.contains("$"))
            {
                continue;
            }

            if (clas.getAnnotation(ModuleUnlisted.class) != null)
            {
                unlistedModules.put(name, clas);
            }
        }

        return unlistedModules;
    }

    public static Module instance(String name)
    {
        try
        {
            Reflections reflections = new Reflections("qit");

            for (final Class<? extends Module> c : reflections.getSubTypesOf(Module.class))
            {
                if (c != null && c.getSimpleName().equals(name))
                {
                    return c.newInstance();
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Logging.error("failed to load commands: " + e.getMessage());
        }
        return null;
    }

    public static List<String> sort(String name)
    {
        Reflections reflections = new Reflections("qit");

        boolean matched = false;
        List<String> contains = Lists.newArrayList();
        List<String> names = Lists.newArrayList();
        for (final Class<? extends Module> c : reflections.getSubTypesOf(Module.class))
        {
            if (c != null)
            {
                String cname = c.getSimpleName();

                if (cname.equals(name))
                {
                    matched = true;
                }
                if (StringUtils.sortWords(cname).contains(StringUtils.sortWords(name)))
                {
                    contains.add(cname);
                }
                else
                {
                    names.add(c.getSimpleName());
                }
            }
        }

        Collections.sort(contains);
        List<String> sorted = StringUtils.sort(names, name);

        List<String> out = Lists.newArrayList();

        if (matched)
        {
            out.add(name);
        }

        out.addAll(contains);
        out.addAll(sorted);

        return out;
    }

    public static void validate(Module module)
    {
        List<String> reports = report(module);

        if (reports.size() > 0)
        {
            for (String report : reports)
            {
                System.out.println(String.format("%s\n", report));
            }

            System.out.println(String.format("Module validation revealed %d invalid fields.  See report results above.\n", reports.size()));
        }

        Global.assume(report(module).size() == 0, String.format("Module '%s' is invalid, see the above messages.", module.getClass().getSimpleName()));
    }

    public static void validate()
    {
        List<String> reports = Lists.newArrayList();
        for (Module module : list())
        {
            reports.addAll(report(module));
        }

        if (reports.size() > 0)
        {
            for (String report : reports)
            {
                System.out.println(String.format("%s\n", report));
            }

            System.out.println(String.format("Module validation revealed %d invalid fields.  See report results above.\n", reports.size()));
        }
        else
        {
            System.out.println("All modules passed validation... congrats!");
        }
    }

    public static List<String> report(Module module)
    {
        List<String> out = Lists.newArrayList();

        try
        {
            module = module.getClass().newInstance();
        }
        catch (Exception e)
        {
            out.add(e.getMessage());
            return out;
        }

        Class<? extends Module> moduleClass = module.getClass();

        fields:
        for (Field field : moduleClass.getDeclaredFields())
        {
            boolean input = field.getAnnotation(ModuleInput.class) != null;
            boolean param = field.getAnnotation(ModuleParameter.class) != null;
            boolean output = field.getAnnotation(ModuleOutput.class) != null;
            boolean advanced = field.getAnnotation(ModuleAdvanced.class) != null;
            boolean optional = field.getAnnotation(ModuleOptional.class) != null;

            if (!input && !param && !output)
            {
                continue fields;
            }

            String fieldName = field.getName();
            Class<?> fieldType = field.getType();

            if (!((input ^ param ^ output) ^ (input && param && output)))
            {
                out.add(String.format("Module: %s\nField: %s\nMessage: must be only one of the following: Input, Parameter, Output.  (hint: pick one)\n", moduleClass.getSimpleName(), fieldName));
                continue fields;
            }

            if (output && advanced)
            {
                out.add(String.format("Module: %s\nField: %s\nMessage: cannot be marked as both output and advanced. (hint: remove @ModuleAdvanced)\n", moduleClass.getSimpleName(), fieldName));
                continue fields;
            }

            Set<Class<?>> dataClasses = Sets.newHashSet();
            dataClasses.add(Affine.class);
            dataClasses.add(Curves.class);
            dataClasses.add(Dataset.class);
            dataClasses.add(Deformation.class);
            dataClasses.add(Gradients.class);
            dataClasses.add(Neuron.class);
            dataClasses.add(Mask.class);
            dataClasses.add(Matrix.class);
            dataClasses.add(Mesh.class);
            dataClasses.add(Solids.class);
            dataClasses.add(Table.class);
            dataClasses.add(Vects.class);
            dataClasses.add(Volume.class);

            if (input || output)
            {
                boolean match = false;
                for (Class<?> c : dataClasses)
                {
                    match |= c.getSimpleName().equals(fieldType.getSimpleName());
                }
                if (!match)
                {
                    out.add(String.format("Module: %s\nField: %s\nMessage: cannot have datatype '%s'. (hint: switch it to a @ModuleParameter)", moduleClass.getSimpleName(), fieldName, fieldType.getSimpleName()));
                    continue fields;
                }
            }

            Set<Class<?>> paramClasses = Sets.newHashSet();
            paramClasses.add(String.class);
            paramClasses.add(boolean.class);
            paramClasses.add(Boolean.class);
            paramClasses.add(int.class);
            paramClasses.add(Integer.class);
            paramClasses.add(double.class);
            paramClasses.add(Double.class);

            if (param)
            {
                if (fieldType.isEnum())
                {
                    continue;
                }

                for (Class<?> c : dataClasses)
                {
                    if (c.getSimpleName().equals(fieldType.getSimpleName()))
                    {
                        out.add(String.format("Module: %s\nField: %s\nMessage: cannot be marked parameter with datatype '%s'. (hint: change it to @ModuleInput)\n", moduleClass.getSimpleName(), fieldName, fieldType.getSimpleName()));
                        continue fields;
                    }
                }

                boolean match = false;
                for (Class<?> c : paramClasses)
                {
                    match |= c.getSimpleName().equals(fieldType.getSimpleName());
                }

                if (!match)
                {
                    out.add(String.format("Module: %s\nField: %s\nMessage: cannot be datatype '%s'. (hint: change it to String, Boolean, Integer, Double, or an Enum)\n", moduleClass.getSimpleName(), fieldName, fieldType.getSimpleName()));
                    continue fields;
                }
            }

            Set<Class<?>> literalClasses = Sets.newHashSet();
            literalClasses.add(boolean.class);
            literalClasses.add(int.class);
            literalClasses.add(double.class);

            if (param && optional)
            {
                if (fieldType.isEnum())
                {
                    out.add(String.format("Module: %s\nField: %s\nMessage: enums cannot be marked as optional (hint: remove @ModuleOptional).", moduleClass.getSimpleName(), fieldName, fieldType.getSimpleName()));
                    continue fields;
                }

                for (Class<?> c : literalClasses)
                {
                    if (c.getSimpleName().equals(fieldType.getSimpleName()))
                    {
                        out.add(String.format("Module: %s\nField: %s\nMessage: cannot be marked as optional with literal datatype '%s'. (hint: change it to the matching object type, e.g. int to Integer).", moduleClass.getSimpleName(), fieldName, fieldType.getSimpleName()));
                        continue fields;
                    }
                }
            }

            boolean bool = Boolean.class.getSimpleName().equals(fieldType.getSimpleName());
            bool |= boolean.class.getSimpleName().equals(fieldType.getSimpleName());

            if (param && bool && optional)
            {
                out.add(String.format("Module: %s\nField: %s\nMessage: booleans should not be marked optional. (hint: a missing value is considered false)\n", moduleClass.getSimpleName(), fieldName));
                continue fields;
            }

            if (bool && (boolean) value(module, field))
            {
                out.add(String.format("Module: %s\nField: %s\nMessage: booleans should not true by default. (hint: this is important for command line interfaces)\n", moduleClass.getSimpleName(), fieldName));
                continue fields;
            }
        }

        return out;
    }

    public static List<Field> fields(Module module)
    {
        Class<? extends Module> type = module.getClass();

        List<Field> out = Lists.newArrayList();
        for (Field field : type.getDeclaredFields())
        {
            boolean input = field.getAnnotation(ModuleInput.class) != null;
            boolean param = field.getAnnotation(ModuleParameter.class) != null;
            boolean output = field.getAnnotation(ModuleOutput.class) != null;

            if (input || param || output)
            {
                out.add(field);
            }
        }

        return out;
    }

    public static List<Field> inputs(Module module)
    {
        Class<? extends Module> type = module.getClass();

        List<Field> out = Lists.newArrayList();
        for (Field field : type.getDeclaredFields())
        {
            if (field.getAnnotation(ModuleInput.class) != null)
            {
                out.add(field);
            }
        }

        return out;
    }

    public static List<Field> parameters(Module module)
    {
        Class<? extends Module> type = module.getClass();

        List<Field> out = Lists.newArrayList();
        for (Field field : type.getDeclaredFields())
        {
            if (field.getAnnotation(ModuleParameter.class) != null)
            {
                out.add(field);
            }
        }

        return out;
    }

    public static List<Field> outputs(Module module)
    {
        Class<? extends Module> type = module.getClass();

        List<Field> out = Lists.newArrayList();
        for (Field field : type.getDeclaredFields())
        {
            if (field.getAnnotation(ModuleOutput.class) != null)
            {
                out.add(field);
            }
        }

        return out;
    }

    public static Object value(Module module, Field field)
    {
        try
        {
            field.setAccessible(true);
            return field.get(module);
        }
        catch (IllegalArgumentException | IllegalAccessException e)
        {
            return null;
        }
    }

    public static void write(Module module, String fn) throws IOException
    {
        ModuleJson out = new ModuleJson();

        out.name = module.getClass().getSimpleName();
        for (Field field : fields(module))
        {
            if (field.getAnnotation(ModuleParameter.class) != null)
            {
                Object value = value(module, field);
                out.parameters.put(field.getName(), value == null ? "" : value.toString());
            }
        }

        FileUtils.write(new File(fn), JsonUtils.encode(out), false);
    }

    public static Module read(String fn) throws IOException
    {
        ModuleJson json = JsonUtils.decode(new ModuleJson().getClass(), Files.toString(new File(fn), Charsets.UTF_8));

        Module module = instance(json.name);

        Global.assume(module != null, "failed to find module: " + json.name);

        for (Field field : fields(module))
        {
            String fieldName = field.getName();
            Class<?> fieldType = field.getType();

            if (field.getAnnotation(ModuleParameter.class) != null && json.parameters.containsKey(fieldName))
            {
                String value = json.parameters.get(fieldName);

                try
                {
                    field.setAccessible(true);
                    if (value == null || value.length() == 0)
                    {
                        field.set(module, null);
                    }
                    else if (fieldType.equals(String.class))
                    {
                        field.set(module, value);
                    }
                    else if (fieldType.equals(Boolean.class) || fieldType.equals(boolean.class))
                    {
                        field.set(module, Boolean.valueOf(value));
                    }
                    else if (fieldType.equals(Integer.class) || fieldType.equals(int.class))
                    {
                        field.set(module, Integer.valueOf(value));
                    }
                    else if (fieldType.equals(Double.class) || fieldType.equals(double.class))
                    {
                        field.set(module, Double.valueOf(value));
                    }
                    else if (fieldType.isEnum())
                    {
                        field.set(module, Enum.valueOf((Class<Enum>) field.getType(), value));
                    }
                    else
                    {
                        throw new RuntimeException("invalid field type: " + fieldType.toString());
                    }
                }
                catch (IllegalAccessException e)
                {
                    Logging.info(String.format("warning: failed to update module '%s' field '%s'", json.name, fieldName));
                }
            }
        }

        return module;
    }

    static class ModuleJson
    {
        String name = "None";
        Map<String, String> parameters = Maps.newLinkedHashMap();
    }
}
