/*
MIT License

Copyright (c) 2025 Maciej Bromirski

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.FileSystem;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Stream;

public class SDKPSubmissionFixer {
    private static boolean fixproject = true;
    private static boolean fixclasspath = true;
    private static boolean fixsubdir = false;
    private static boolean fixnoroot = true;
    private static boolean removeMac = true;
    private static boolean fixauthors = true;
    private static boolean deletebadroots = true;
    private static boolean keeporiginals = false;

    private static String s_project = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<projectDescription>\n"
        + "  <name>$$SDKPSTRING$$</name>\n"
        + "  <comment></comment>\n"
        + "  <projects>\n"
        + "  </projects>\n"
        + "  <buildSpec>\n"
        + "    <buildCommand>\n"
        + "      <name>org.eclipse.jdt.core.javabuilder</name>\n"
        + "      <arguments>\n"
        + "      </arguments>\n"
        + "    </buildCommand>\n"
        + "  </buildSpec>\n"
        + "  <natures>\n"
        + "    <nature>org.eclipse.jdt.core.javanature</nature>\n"
        + "  </natures>\n"
        + "</projectDescription>\n";

    private static String s_classpath = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<classpath>\n"
        + "  <classpathentry kind=\"src\" path=\"src\"/>\n"
        + "  <classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER\"/>\n"
        + "  <classpathentry kind=\"output\" path=\"bin\"/>\n"
        + "</classpath>\n";

    public static class SubmissionFile {
        private String subject;
        private int nlab;
        private String initials;
        private String sindex;
        private String surname;
        private String name;

        public SubmissionFile(String subject, int nlab, String name, String surname, String sindex) {
            this.subject = subject;
            this.nlab = nlab;
            this.surname = surname;
            this.name = name;
            this.initials = "" + surname.charAt(0) + name.charAt(0);
            this.sindex = sindex;
        }

        public String SDKPString() {
            return this.subject.toUpperCase()
                + this.nlab + "_"
                + this.initials.toUpperCase() + "_"
                + this.sindex.toUpperCase();
        }

        public String JDAString() {
            String ans = "/**\n *\n *  @author $$SURNAME$$ $$NAME$$ $$SINDEX$$\n *\n *\n */";
            ans = ans.replace("$$SURNAME$$", this.surname);
            ans = ans.replace("$$NAME$$", this.name);
            ans = ans.replace("$$SINDEX$$", this.sindex.toUpperCase());
            return ans;
        }
    }

    public static void fixSubmission(SubmissionFile sf, String operation_fp) {
        if (!Files.exists(Paths.get(operation_fp))) { return; }

        operation_fp = operation_fp.replace(".zip", "-op.zip");
        String original_fp = operation_fp.replace("-op.zip", ".zip");
        try { SDKPSubmissionFixer.duplicateFile(original_fp, operation_fp); } catch (IOException _ex) { return; }

        HashMap<String, String> env = new HashMap<String, String>();
        env.put("create", "true");
        Path path = Paths.get(operation_fp);
        URI uri = URI.create("jar:" + path.toUri());
        try (FileSystem fs = FileSystems.newFileSystem(uri, env)) {
            if (SDKPSubmissionFixer.removeMac) { // -- Usuń wszystki pliki macOS
                if (Files.exists(fs.getPath("__MACOSX"))) {
                    SDKPSubmissionFixer.deleteFile(fs, "__MACOSX");
                }
                for (Path p : Files.walk(fs.getPath("")).toList()) {
                    if (p.toString().isEmpty()) { continue; }
                    String filename = p.getFileName().toString();
                    if (filename.equals(".DS_Store") || filename.equals("._.DS_Store")) {
                        SDKPSubmissionFixer.deleteFile(fs, p.toString());
                    }
                }
            }
            if (!Files.isDirectory(fs.getPath(sf.SDKPString()))) {
                Files.createDirectories(fs.getPath(sf.SDKPString()));
            }
            String inner_fp = sf.SDKPString();
            if (SDKPSubmissionFixer.fixnoroot) {
                for (Path p : Files.walk(fs.getPath("")).toList()) {
                    if (p.toString().isEmpty()) { continue; }
                    if (p.startsWith(sf.SDKPString())) { continue; }
                    if (p.toString().equals(Paths.get(operation_fp).getFileName().toString().replace("-op.zip", ""))) { continue; }
                    SDKPSubmissionFixer.moveFile(fs, p.toString(), sf.SDKPString() + "/" + SDKPSubmissionFixer.tailPath(p));
                }
            }
            if (SDKPSubmissionFixer.deletebadroots) {
                for (Path p : Files.walk(fs.getPath(""), 1).toList()) {
                    if (p.toString().isEmpty()) { continue; }
                    if (p.toString().startsWith(sf.SDKPString())) { continue; }
                    SDKPSubmissionFixer.deleteFile(fs, p.toString());
                }
            }
            if (SDKPSubmissionFixer.fixsubdir) {
                // todo: if src, etc exist in a subdirectory, move them to the root directory
            }
            if (SDKPSubmissionFixer.fixproject) {
                SDKPSubmissionFixer.writeFile(fs, inner_fp + "/.project",
                    SDKPSubmissionFixer.s_project.replace("$$SDKPSTRING$$", sf.SDKPString())
                );
            }
            if (SDKPSubmissionFixer.fixclasspath) {
                SDKPSubmissionFixer.writeFile(fs, inner_fp + "/.classpath", SDKPSubmissionFixer.s_classpath);
            }
            if (SDKPSubmissionFixer.fixauthors) { // -- Dodaj javadoc @author dla każdego pliku .java w src/
                // todo: optionally fix wrong ordering in author too
                if (Files.exists(fs.getPath(inner_fp + "/src"))) {
                    Stream<Path> files = Files.walk(fs.getPath(inner_fp + "/src"))
                            .filter(f -> !Files.isDirectory(f) && f.getFileName().toString().endsWith(".java"));
                    for (Path p : files.toList()) {
                        try {
                            String fcontents = new String(Files.readAllBytes(p));
                            Pattern pattern = Pattern.compile("/\\*\\* *\\n(?: ?\\*.*\\n)* ?\\*/");
                            Matcher matcher = pattern.matcher(fcontents);
                            if (!(matcher.find() && matcher.start() == 0)) {
                                fcontents = sf.JDAString() + "\n" + fcontents;
                                SDKPSubmissionFixer.writeFile(fs, p.toString(), fcontents);
                            }
                        } catch (IOException ex) { return; }
                    }
                }
            }
        } catch (IOException ex) { return; }
        try {
            SDKPSubmissionFixer.duplicateFile(operation_fp, original_fp);
            if (SDKPSubmissionFixer.keeporiginals) {
                SDKPSubmissionFixer.moveFile(original_fp, operation_fp.replace("-op.zip", ".og.zip"));
            }
            SDKPSubmissionFixer.deleteFile(operation_fp);
        } catch (IOException ex) { return; }
    }

    private static String tailPath(Path p) {
        return p.getNameCount() <= 1 ? p.toString() : p.subpath(1, p.getNameCount()).toString();
    }
    private static void duplicateFile(String fp_original, String fp_copy) throws IOException {
        Files.copy(Paths.get(fp_original), Paths.get(fp_copy), StandardCopyOption.REPLACE_EXISTING);
    }
    private static void moveFile(FileSystem fs, String fp_original, String fp_move) throws IOException {
        Files.move(fs.getPath(fp_original), fs.getPath(fp_move), StandardCopyOption.REPLACE_EXISTING);
    }
    private static void moveFile(String fp_original, String fp_move) throws IOException {
        Files.move(Paths.get(fp_original), Paths.get(fp_move), StandardCopyOption.REPLACE_EXISTING);
    }
    private static void deleteFile(String fp) throws IOException {
        Path toDelete = Paths.get(fp);
        SDKPSubmissionFixer._deleteFile(toDelete);
    }
    private static void deleteFile(FileSystem fs, String fp) throws IOException {
        Path toDelete = fs.getPath(fp);
        SDKPSubmissionFixer._deleteFile(toDelete);
    }
    private static void _deleteFile(Path toDelete) throws IOException {
        if (Files.isDirectory(toDelete)) {
            Files.walkFileTree(toDelete,
                new SimpleFileVisitor<Path>() {
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                }
            );
        } else {
            Files.delete(toDelete);
        }
    }
    private static void writeFile(FileSystem fs, String fp, String contents) throws IOException {
        Path f = fs.getPath(fp);
        try (Writer writer = Files.newBufferedWriter(f, StandardCharsets.UTF_8, StandardOpenOption.CREATE)) {
            writer.write(contents);
        }
    }
}