package com.simonbuckle.ant.tasks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Mapper;

import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;
import com.yahoo.platform.yui.compressor.org.mozilla.javascript.ErrorReporter;
import com.yahoo.platform.yui.compressor.org.mozilla.javascript.EvaluatorException;

/**
 * @author Simon Buckle
 */

public class CompressTask extends Task {

	private List<FileSet> filesets = new ArrayList<FileSet>();
	private Mapper mapper;
	private int linebreak = -1;
	private boolean munge = true;
	private boolean preserveAllSemiColons = false;
	private boolean disableOptimizations = false;
	private boolean verbose = false;
	private String todir;

	public void addFileset(FileSet fileset) {
		filesets.add(fileset);
	}

	public void addMapper(Mapper mapper) {
		this.mapper = mapper;
	}

	public void setDisableOptimizations(boolean disableOptimizations) {
		this.disableOptimizations = disableOptimizations;
	}

	public void setLinebreak(int linebreak) {
		this.linebreak = linebreak;
	}

	public void setMunge(boolean munge) {
		this.munge = munge;
	}

	public void setPreserveAllSemiColons(boolean preserveAllSemiColons) {
		this.preserveAllSemiColons = preserveAllSemiColons;
	}

	public void setToDir(String todir) {
		this.todir = todir;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	private void validateRequired() throws BuildException {
		StringBuilder errorString = new StringBuilder();

		if (mapper == null)
			errorString.append("Mapper property is required\n");
		if (todir == null || "".equals(todir))
			errorString.append("Output directory is not specified\n");

		if (errorString.length()>0) {
			throw new BuildException(errorString.toString());
		}
	}

	public void execute() throws BuildException {
		validateRequired();

		Iterator<FileSet> iter = filesets.listIterator();
		while (iter.hasNext()) {
			FileSet fileset = iter.next();
			DirectoryScanner scanner = fileset.getDirectoryScanner(getProject());
			File dir = scanner.getBasedir();

			String[] files = scanner.getIncludedFiles();
			for (int i = 0; i < files.length; i++) {
				String fileName = files[i];
				String[] output = mapper.getImplementation().mapFileName(fileName);
				if (output != null) {
					try {
						if (fileName.endsWith("css")) {
							compressCss(new File(dir, fileName), new File(todir, output[0]));
						} else {
							compress(new File(dir, fileName), new File(todir, output[0]));
						}

					} catch (IOException io) {
						log("Failed to compress file: " + fileName);
					}
				}
			}
		}
	}

	private void compress(File source, File dest) throws IOException {
		Reader in = null;
		Writer out = null;
		try {
			in = new BufferedReader(new FileReader(source));
			JavaScriptCompressor compressor = new JavaScriptCompressor(in, new ErrorReporter() {

				public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) { 
					log("Warning: " + message, Project.MSG_WARN);
				}

				public void error(String message, String sourceName, int line, String lineSource, int lineOffset) { 
					log("Error: " + message, Project.MSG_ERR);
				}

				public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int lineOffset) { 
					return new EvaluatorException(message);
				}

			});

			out = new BufferedWriter(new FileWriter(dest));
			log("Compressing: " + source.getName());

			compressor.compress(out, 
					out, linebreak, 
					munge,
					verbose,
					preserveAllSemiColons,
					disableOptimizations);
		} finally {
			if (in != null) in.close();
			if (out != null) out.close();
		}
	}

	private void compressCss(File source, File dest) throws IOException {
		Reader in = null;
		Writer out = null;
		try {
			in = new BufferedReader(new FileReader(source));
			CssCompressor compressor = new CssCompressor(in);

			log("Compressing: " + source.getName());

			out = new BufferedWriter(new FileWriter(dest));
			compressor.compress(out, linebreak);

		} finally {
			if (in != null) in.close();
			if (out != null) out.close();
		}
	}
}
