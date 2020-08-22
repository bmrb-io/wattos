/*
 *This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 *All Rights Reserved. No warranty implied or expressed.
 */
package Wattos.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Class to capture i/o. Adapted from Michael Daconta's {@link <a
 * href='http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html'>
 * paper</a>}.
 *
 * @author Michael Daconta
 * @author Jurgen F. Doreleijers
 * @version 0.1
 */
class StreamGobbler extends Thread {
    InputStream is;
    String type;

    StreamGobbler(InputStream is, String type) {
        this.is = is;
        this.type = type;
    }

    /**
     * Kick off the stream gobbling.
     */
    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null)
                General.showOutput(type + ">" + line);
        } catch (IOException t) {
            General.showThrowable(t);
        }
    }
}

class StreamBuffer extends Thread {
    InputStream is;
    String type;
    StringBuffer sbuffer = new StringBuffer();

    StreamBuffer(InputStream is, String type) {
        this.is = is;
        this.type = type;
    }

    /**
     * Kick off the stream gobbling.
     */
    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null) {
                sbuffer.append(line + '\n');
//                General.showOutput(type + "sb>" + line);
            }
        } catch (IOException t) {
            General.showThrowable(t);
        }
    }

    /**
     * Call -after- finishing process completely an waiting a few milliseconds.
     *
     * @return
     */
    public String getString() {
        return this.sbuffer.toString();
    }
}

/**
 * Execute a command in a system dependent way. see #exec
 */
public class OSExec {

    /**
     * Execute in a system dependent way. So make sure you give the appropriate
     * command. I.e. don't try to 'ls' on a Windows system without such a
     * command.
     *
     * @param args
     *            Command to be executed. Examples that work are given below. On
     *            Linux: "nedit -geometry 80x50-0+0" On Windows:
     *            "\"D:\\Program Files\\Microsoft Office\\Office\\Winword.exe\""
     * @return Exit status of the command. 0 usually means success. What is
     *         usually?
     */
    public static int exec(String args) {
        // Standard error exit value
        int exitVal = 9;

        try {
            String osName = System.getProperty("os.name");
            // General.showOutput("In OS: " + osName);

            String[] cmd = new String[3];
            if (osName.equals("Windows NT") || osName.equals("Windows 2000") || osName.equals("Windows XP")) {
                cmd[0] = "cmd.exe";
                cmd[1] = "/C";
            } else if (osName.equals("Windows 95")) {
                cmd[0] = "command.com";
                cmd[1] = "/C";
            } else if (osName.equals("Linux") || osName.equals("SunOS") || osName.startsWith("Mac OS")) {
                // It's a unix variant
                cmd[0] = "/bin/csh";
                cmd[1] = "-c";
            } else {
                General.showOutput("Any Operating System that is not:");
                General.showOutput("- Windows");
                General.showOutput("- Linux, SunOS, or Mac OS");
                General.showOutput("is currently not supported by Wattos.Utils.OSExec.");
                General.showOutput("The OS of this machine is determined by Java as: " + osName);
                return exitVal;
            }
            cmd[2] = args;

            Runtime rt = Runtime.getRuntime();
            // General.showOutput("Execing: " + cmd[0].toString());
            // General.showOutput("Execing: " + cmd[1].toString());
            // General.showOutput("Execing: " + cmd[2].toString());
            Process proc = rt.exec(cmd);
            // any error message?
            StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), "STDERR");

            // any output?
            StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), "STDOUT");

            // kick them off
            errorGobbler.start();
            outputGobbler.start();

            // any error???
            exitVal = proc.waitFor();
            // General.showOutput("ExitValue: " + exitVal);
        } catch (Throwable t) {
            General.showThrowable(t);
        }
        return (exitVal);
    }

    /**
     * See namesake python method: commands#getstatusoutput but also include error output in separate String.
     *
     * @return strings for status and output of given shell command. Or null in
     *         case of error. Exit status is the String representation that can
     *         be parsed to an int.
     *
     *         status, outputMsg, errorMsg
     */
    public static String[] getstatusoutput(String args) {
        String errorMsg;
        String outputMsg;
        String status;
        try {
            String osName = System.getProperty("os.name");
            // General.showOutput("In OS: " + osName);

            String[] cmd = new String[3];
            if (osName.equals("Windows NT") || osName.equals("Windows 2000") || osName.equals("Windows XP")) {
                cmd[0] = "cmd.exe";
                cmd[1] = "/C";
            } else if (osName.equals("Windows 95")) {
                cmd[0] = "command.com";
                cmd[1] = "/C";
            } else if (osName.equals("Linux") || osName.equals("SunOS") || osName.startsWith("Mac OS")) {
                // It's a unix variant
                cmd[0] = "/bin/csh";
                cmd[1] = "-c";
            } else {
                General.showOutput("Any Operating System that is not:");
                General.showOutput("- Windows");
                General.showOutput("- Linux, SunOS, or Mac OS");
                General.showOutput("is currently not supported by Wattos.Utils.OSExec.");
                General.showOutput("The OS of this machine is determined by Java as: " + osName);
                return null;
            }
            cmd[2] = args;

            Runtime rt = Runtime.getRuntime();
            // General.showOutput("Execing: " + cmd[0].toString());
            // General.showOutput("Execing: " + cmd[1].toString());
            // General.showOutput("Execing: " + cmd[2].toString());
            Process proc = rt.exec(cmd);
            // any error message?
            StreamBuffer errorGobbler = new StreamBuffer(proc.getErrorStream(), "STDERR");

            // any output?
            StreamBuffer outputGobbler = new StreamBuffer(proc.getInputStream(), "STDOUT");

            // kick them off
            errorGobbler.start();
            outputGobbler.start();

            // any error???
            status = String.valueOf(  proc.waitFor() );
            Thread.sleep(300);
            errorMsg = errorGobbler.getString();
            outputMsg = outputGobbler.getString();
//            General.showOutput("status: " + status);
//            General.showOutput("errorMsg: " + errorMsg);
//            General.showOutput("outputMsg: " + outputMsg);
        } catch (Throwable t) {
            General.showThrowable(t);
            return null;
        }
        return new String[] { status, outputMsg, errorMsg };
    }

    /**
     * Self test; executes a simple "dir" command. If the command doesn't exist
     * on the machine an error will be issued. On both my Windows and Linux
     * machine it does however.
     *
     * @param args
     *            Command line arguments.
     */
    public static void main(String args[]) {
        if (args.length < 0) {
            General.showOutput("USAGE: java Wattos.Utils.OSExec <cmd>");
            System.exit(1);
        }

        // The following command exists on Windows and my Linux box
        // String infile =
        // exec("format ");
        // exec("java -version");
        // exec(
        // "\"C:\\Progra~1\\jEdit4~1.1pr\\jedit.exe i:\\pdbmirror2\\nozip\\data\\structures\\all\\nmr_restraints\\1brv.mr\""
        // );
        // "C:\Progra~1\jEdit4~1.1pr\jedit.exe
        // i:\pdbmirror2\nozip\data\structures\all\nmr_restraints\1brv.mr
        // exec("\"dir i:\\pdbmirror2\\nozip\\data\\structures\\all\\nmr_restraints\\1brv.mr\"");
        // /
        // i:\\pdbmirror2\\nozip\\data\\structures\\all\\nmr_restraints\\1brv.mr");
        // exec("\"C:\\Program Files\\Microsoft Office\\Office\\Winword.exe\"");
        // exec("java -Xmx96m -Xms24m -jar S:\\linux\\src\\jedit\\4.0.3\\jedit.jar");
        // String cmd = "\"C:\\Program Files\\jEdit 4.1\\jedit.exe\" inputFile";
        String cmd = args[0];
        int status = exec(cmd);
        if (status != 0) {
            General.showError("Found an error exit status: " + status);
            System.exit(status);
        } else {
            General.showOutput("Finished OSExec successfully");
        }
    }

    /**
     * Convenience method to namesake method.
     *
     * @param cmdList
     * @param delayBetweenSubmittingJobs
     *            in milliseconds.
     * @return Exit status of the command. 0 usually means success. 1 for
     *         failure.
     */
    public static int exec(String[] cmdList, int delayBetweenSubmittingJobs) {
        int status = 0;
        int i = 0;
        int iLast = cmdList.length - 1;
        int iLastSuccess = -1;
        for (; i <= iLast; i++) {
            status = exec(cmdList[i]);
            if (status != 0) {
                General.showError("Failed to execute command: [" + cmdList[i] + "]");
                General.showError("Found an error exit status: " + status);
                break;
            }
            iLastSuccess = i;
            // Continue right away after last one.
            if (i != iLast) {
                General.sleep(delayBetweenSubmittingJobs);
            }
        }
        if (status != 0) {
            General.showError("Only " + (iLastSuccess + 1) + " out of " + cmdList.length
                    + " jobs were started (not all successfully finished perhaps)");
            return 1;
        }
        General.showOutput("Finished " + (iLastSuccess + 1) + " out of the " + cmdList.length
                + " processes successfully.");
        return 0;
    }
}
