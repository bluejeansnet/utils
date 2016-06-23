/*
 * Copyright Blue Jeans Network.
 */
package com.bluejeans.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;

/**
 * JVM and OS info
 *
 * @author Dinesh Ilindra
 */
public class SystemInfo {

    /**
     * The current OS line separator.
     */
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");

    /**
     * The JVM process id.
     */
    public static final String PROCESS_ID = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];

    /**
     * Returns the JVM runtime.
     *
     * @return the runtime
     */
    public Runtime getRuntime() {
        return Runtime.getRuntime();
    }

    /**
     * Returns the runtime MX bean.
     *
     * @return the runtime MX bean
     */
    public RuntimeMXBean getRuntimeMXBean() {
        return ManagementFactory.getRuntimeMXBean();
    }

    /**
     * Returns the thread MX bean.
     *
     * @return the thread MX bean
     */
    public ThreadMXBean getThreadMXBean() {
        return ManagementFactory.getThreadMXBean();
    }

    /**
     * Returns the memory MX bean.
     *
     * @return the memory MX bean
     */
    public MemoryMXBean getMemoryMXBean() {
        return ManagementFactory.getMemoryMXBean();
    }

    /**
     * Returns the operating system MX bean.
     *
     * @return the operating system MX bean
     */
    public OperatingSystemMXBean getOsMXBean() {
        return ManagementFactory.getOperatingSystemMXBean();
    }

    /**
     * Returns the classloading MX bean.
     *
     * @return the classloading MX bean
     */
    public ClassLoadingMXBean getClassLoadingMXBean() {
        return ManagementFactory.getClassLoadingMXBean();
    }

    /**
     * Returns the compilation MX bean.
     *
     * @return the compilation MX bean
     */
    public CompilationMXBean getCompilationMXBean() {
        return ManagementFactory.getCompilationMXBean();
    }

    /**
     * Runs a command from its stream data.
     *
     * @param commandStream
     *            the command stream
     * @param runName
     *            the run name
     * @param arguments
     *            the arguments
     * @param deleteOnExit
     *            if should delete on exit?
     * @return the process object
     */
    public static Process runCommandFromStream(final InputStream commandStream, final String runName,
            final String arguments, final boolean deleteOnExit) {
        Process process = null;
        try {
            final File runFile = new File("/tmp/" + runName);
            final FileOutputStream outStream = new FileOutputStream(runFile);
            IOUtils.copy(commandStream, outStream);
            outStream.flush();
            outStream.close();
            if (deleteOnExit) {
                runFile.deleteOnExit();
            }
            runFile.setExecutable(true);
            process = Runtime.getRuntime().exec(runFile.getAbsolutePath() + " " + arguments);
        } catch (final Throwable th) {
            th.printStackTrace();
        }
        return process;
    }

    private final Map<String, Process> processMap = new HashMap<String, Process>();

    /**
     * Returns the process id.
     *
     * @return the process id
     */
    public String getProcessId() {
        return SystemInfo.PROCESS_ID;
    }

    /**
     * @return the processMap
     */
    public Map<String, Process> getProcessMap() {
        return processMap;
    }

    /**
     * Writes a line to the command stream.
     *
     * @param command
     *            the command
     * @param line
     *            the line to send
     */
    public void writeLine(final String command, final String line) {
        writeLine(processMap.get(command), line);
    }

    /**
     * Writes a line to the command stream.
     *
     * @param process
     *            the process
     * @param line
     *            the line to send
     */
    public void writeLine(final Process process, final String line) {
        new PrintStream(process.getOutputStream()).println(line);
    }

    /**
     * Reads a line from the command stream.
     *
     * @param command
     *            the command
     * @return a line from output
     * @throws IOException
     *             implicit
     */
    public String readLine(final String command) throws IOException {
        return readLine(processMap.get(command));
    }

    /**
     * Reads a line from the command stream.
     *
     * @param process
     *            the process
     * @return a line from output
     * @throws IOException
     *             implicit
     */
    public String readLine(final Process process) throws IOException {
        return new BufferedReader(new InputStreamReader(process.getInputStream())).readLine();
    }

    /**
     * Reads all the output lines from the command stream.
     *
     * @param command
     *            the command
     * @return the lines from output
     * @throws IOException
     *             implicit
     */
    public String readLines(final String command) throws IOException {
        return readLines(processMap.get(command));
    }

    /**
     * Reads all the output lines from the command stream.
     *
     * @param process
     *            the process
     * @return the lines from output
     * @throws IOException
     *             implicit
     */
    public String readLines(final Process process) throws IOException {
        final StringBuilder builder = new StringBuilder();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String outLine = null;
        builder.append(SystemInfo.LINE_SEPARATOR);
        while ((outLine = reader.readLine()) != null) {
            builder.append(outLine);
            builder.append(SystemInfo.LINE_SEPARATOR);
        }
        return builder.toString();
    }

    /**
     * Executes a command sends the given input line and returns the output of
     * it.
     *
     * @param command
     *            the command
     * @param line
     *            the line to send
     * @return the output
     * @throws IOException
     *             implicit
     */
    public String execOut(final String command, final String line) throws IOException {
        return execOut(command, null, null, line, true);
    }

    /**
     * Executes a command sends the given input line and returns the output of
     * it.
     *
     * @param command
     *            the command
     * @param envp
     *            the environment list
     * @param dir
     *            the directory
     * @param line
     *            the line to send
     * @param destroy
     *            should destroy process after done?
     * @return the output
     * @throws IOException
     *             implicit
     */
    public String execOut(final String command, final String[] envp, final File dir, final String line,
            final boolean destroy) throws IOException {
        final Process process = exec(command, envp, dir);
        writeLine(command, line);
        final String lines = readLines(process);
        if (destroy) {
            process.destroy();
            processMap.remove(command);
        }
        return lines;
    }

    /**
     * Executes a command and returns the output of it.
     *
     * @param command
     *            the command
     * @return the output
     * @throws IOException
     *             implicit
     */
    public String execOut(final String command) throws IOException {
        return execOut(command, null, null, true);
    }

    /**
     * Executes a command and returns the output of it / exception if any
     *
     * @param command
     *            the command
     * @return the output / stacktrace
     */
    public String run(final String command) {
        try {
            return execOut(command);
        } catch (final Exception ex) {
            final StringWriter writer = new StringWriter();
            ex.printStackTrace(new PrintWriter(writer));
            return writer.toString();
        }
    }

    /**
     * Executes a command and returns the output of it.
     *
     * @param command
     *            the command
     * @param envp
     *            the environment
     * @param dir
     *            the directory
     * @param destroy
     *            destroy after completed?
     * @return the output
     * @throws IOException
     *             implicit
     */
    public String execOut(final String command, final String[] envp, final File dir, final boolean destroy)
            throws IOException {
        final Process process = exec(command, envp, dir);
        final String lines = readLines(process);
        if (destroy) {
            process.destroy();
            processMap.remove(command);
        }
        return lines;
    }

    /**
     * Executes a command.
     *
     * @param command
     *            the command
     * @return the process
     * @throws IOException
     *             implicit
     */
    public Process exec(final String command) throws IOException {
        return exec(command, null, null);
    }

    /**
     * Executes a command.
     *
     * @param command
     *            the command
     * @param envp
     *            the environment
     * @param dir
     *            the directory
     * @return the output
     * @throws IOException
     *             implicit
     */
    public Process exec(final String command, final String[] envp, final File dir) throws IOException {
        final Process proc = getRuntime().exec(command, envp, dir);
        processMap.put(command, proc);
        return proc;
    }

}
