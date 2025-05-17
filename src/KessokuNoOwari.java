import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * KessokuNoOwari - A simple Gradle launcher that handles Java version issues
 * Usage:
 *   - To build: kessoku build
 *   - To run Gradle commands: kessoku run [gradle-command]
 */
public class KessokuNoOwari {
    
    // Possible Java installation locations
    private static final String[] WINDOWS_JAVA_PATHS = {
        "C:\\Program Files\\Java\\jdk-17",
        "C:\\Program Files\\Java\\jdk-11",
        "C:\\Program Files\\Java\\jdk1.8.0",
        "C:\\Program Files (x86)\\Java\\jdk-17",
        "C:\\Program Files (x86)\\Java\\jdk-11"
    };
    
    private static final String[] UNIX_JAVA_PATHS = {
        "/usr/lib/jvm/java-17-openjdk",
        "/usr/lib/jvm/java-11-openjdk",
        "/usr/lib/jvm/java-8-openjdk",
        "/usr/local/openjdk-17",
        "/usr/local/openjdk-11",
        "/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home",
        "/Library/Java/JavaVirtualMachines/jdk-11.jdk/Contents/Home"
    };
    
    public static void main(String[] args) {
        try {
            System.out.println("KessokuNoOwari - Gradle Launcher");
            System.out.println("---------------------------------");
            
            // Find Java Home
            String javaHome = findJavaHome();
            
            if (javaHome != null) {
                System.out.println("Using Java Home: " + javaHome);
            } else {
                System.out.println("Warning: Could not find Java Home. Using system Java.");
            }
            
            // Find Gradle wrapper
            String gradleWrapper = findGradleWrapper();
            System.out.println("Using Gradle: " + gradleWrapper);
            
            // Create command
            List<String> command = buildCommand(javaHome, gradleWrapper, args);
            
            // Display command
            System.out.println("Running command: " + String.join(" ", command));
            
            // Execute from the project root directory, not the bin directory
            ProcessBuilder pb = new ProcessBuilder(command);
            
            // Set working directory to project root
            File projectRoot = findProjectRoot();
            if (projectRoot != null) {
                pb.directory(projectRoot);
                System.out.println("Working directory: " + projectRoot.getAbsolutePath());
            }
            
            pb.inheritIO();
            Process process = pb.start();
            
            // Wait for process to complete
            int exitCode = process.waitFor();
            System.exit(exitCode);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static String findJavaHome() {
        // Check environment variable first
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null && !javaHome.isEmpty() && new File(javaHome).exists()) {
            return javaHome;
        }
        
        // Try to find Java installation
        String[] paths = isWindows() ? WINDOWS_JAVA_PATHS : UNIX_JAVA_PATHS;
        for (String path : paths) {
            File javaDir = new File(path);
            if (javaDir.exists() && new File(javaDir, "bin/java" + (isWindows() ? ".exe" : "")).exists()) {
                return path;
            }
        }
        
        // Try to infer from java command
        try {
            String javaCommand = isWindows() ? "where java" : "which java";
            Process process = Runtime.getRuntime().exec(javaCommand);
            
            // Read the output
            java.io.InputStream is = process.getInputStream();
            byte[] buffer = new byte[1024];
            int bytesRead = is.read(buffer);
            String javaPath = "";
            if (bytesRead > 0) {
                javaPath = new String(buffer, 0, bytesRead).trim();
            }
            
            if (!javaPath.isEmpty()) {
                File javaFile = new File(javaPath);
                if (javaFile.exists()) {
                    // Try to get parent directory of bin
                    File binDir = javaFile.getParentFile();
                    if (binDir != null && binDir.getName().equals("bin")) {
                        return binDir.getParent();
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors in this step
        }
        
        return null;
    }
    
    private static String findGradleWrapper() {
        // Check for gradlew in current directory
        String wrapper = isWindows() ? "gradlew.bat" : "./gradlew";
        File wrapperFile = new File(isWindows() ? "gradlew.bat" : "gradlew");
        
        if (wrapperFile.exists()) {
            return wrapper;
        }
        
        // Look in parent directories
        File current = new File(".").getAbsoluteFile();
        while (current != null) {
            File gradleWrapperFile = new File(current, isWindows() ? "gradlew.bat" : "gradlew");
            if (gradleWrapperFile.exists()) {
                return gradleWrapperFile.getAbsolutePath();
            }
            current = current.getParentFile();
        }
        
        // Fallback to system gradle
        return isWindows() ? "gradle.bat" : "gradle";
    }
    
    /**
     * Find the project root directory (where the gradlew file is located)
     */
    private static File findProjectRoot() {
        // Start with current directory
        File current = new File(".").getAbsoluteFile();
        
        // Check for gradlew in current directory
        if (new File(current, isWindows() ? "gradlew.bat" : "gradlew").exists()) {
            return current;
        }
        
        // Look in parent directories
        while (current != null) {
            File gradleWrapperFile = new File(current, isWindows() ? "gradlew.bat" : "gradlew");
            if (gradleWrapperFile.exists()) {
                return current;
            }
            current = current.getParentFile();
        }
        
        // If we can't find it, just return the current directory
        return new File(".").getAbsoluteFile();
    }
    
    private static List<String> buildCommand(String javaHome, String gradleWrapper, String[] gradleArgs) {
        List<String> command = new ArrayList<>();
        
        if (isWindows()) {
            command.add("cmd");
            command.add("/c");
            
            if (javaHome != null && !javaHome.isEmpty()) {
                command.add("set");
                command.add("JAVA_HOME=" + javaHome);
                command.add("&&");
            }
            
            command.add(gradleWrapper);
            
            // Add Gradle arguments
            for (String arg : gradleArgs) {
                command.add(arg);
            }
            
        } else {
            command.add("/bin/sh");
            command.add("-c");
            
            StringBuilder shellCommand = new StringBuilder();
            
            if (javaHome != null && !javaHome.isEmpty()) {
                shellCommand.append("export JAVA_HOME='").append(javaHome).append("' && ");
            }
            
            shellCommand.append(gradleWrapper);
            
            // Add Gradle arguments
            for (String arg : gradleArgs) {
                shellCommand.append(" '").append(arg.replace("'", "\\'")).append("'");
            }
            
            command.add(shellCommand.toString());
        }
        
        return command;
    }
    
    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}