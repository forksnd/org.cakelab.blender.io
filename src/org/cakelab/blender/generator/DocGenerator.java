package org.cakelab.blender.generator;

import java.io.File;
import java.io.IOException;

import org.cakelab.blender.doc.Documentation;
import org.cakelab.blender.doc.DocumentationProvider;
import org.cakelab.blender.generator.utils.JavaDocPostprocessor;
import org.cakelab.blender.metac.CMetaModel;
import org.cakelab.json.JSONException;

/**
 * DocGenerator is basically a hub to multiple documentations 
 * on the same subject. It searches for documentation in 
 * all sources and combines it into a single documentation.
 * 
 * @author homac
 *
 */
public class DocGenerator implements DocumentationProvider {
	private Documentation[] docs;
	private JavaDocPostprocessor postprocessor;
	
	public DocGenerator(File[] docfiles, CMetaModel model, boolean debug) throws IOException, JSONException {
		postprocessor = new JavaDocPostprocessor(model);
		docs = new Documentation[docfiles.length];
		int i = 0;
		for (File f : docfiles) {
			docs[i++] = new Documentation(f, debug);
		}
	}

	@Override
	public String getStructDoc(String struct) {
		StringBuffer lines = new StringBuffer();
		for (Documentation doc : docs) {
			String docentry = doc.getStructDoc(struct);
			if (docentry != null && docentry.length() > 0) {
				lines.append("<h4>" + doc.getSource() + ":</h4>").append("\n");
				lines.append(docentry);
			}
		}
		return postprocessor.repairDanglingLinks(lines.toString(), struct);
	}

	@Override
	public String getFieldDoc(String struct, String field) {
		StringBuffer lines = new StringBuffer();
		
		for (Documentation doc : docs) {
			String docentry = doc.getFieldDoc(struct, field);
			if (docentry != null && docentry.length() > 0) {
				lines.append("<h4>" + doc.getSource() + ":</h4>").append("\n");
				lines.append(docentry);
			}
		}
		return postprocessor.repairDanglingLinks(lines.toString(), struct);
	}

}
