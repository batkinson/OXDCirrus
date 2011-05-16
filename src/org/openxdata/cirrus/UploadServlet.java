package org.openxdata.cirrus;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.Blob;

@SuppressWarnings("serial")
public class UploadServlet extends HttpServlet {

	private static final Logger log = Logger.getLogger(UploadServlet.class
			.getName());

	@SuppressWarnings("unchecked")
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		String studyPath = req.getPathInfo();

		if (studyPath == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
					"Must specify study path.");
		}

		PersistenceManager pm = PMF.get().getPersistenceManager();

		try {

			byte[] contents = new byte[req.getContentLength()];

			int totalRead = 0;
			int readBytes;
			do {
				readBytes = req.getInputStream().read(contents, totalRead,
						contents.length - totalRead);
				if (readBytes == -1)
					throw new ServletException(
							"Failed to read entire contents: end of stream");
				totalRead += readBytes;
			} while (totalRead < contents.length);

			Query query = pm.newQuery(Form.class);
			query.setFilter("path == pathParam");
			query.declareParameters("String pathParam");
			query.setRange(0, 1);

			List<Form> forms = (List<Form>) query.execute(studyPath);
			Form form;
			if (!forms.isEmpty()) {
				form = forms.get(0);
				form.setFormContent(new Blob(contents));
			} else {
				form = new Form();
				Blob contentBlob = new Blob(contents);
				form.setFormContent(contentBlob);
				form.setPath(studyPath);
			}
			pm.makePersistent(form);
		} catch (Exception e) {
			log.log(Level.WARNING,
					"Failed to upload form for path " + req.getPathInfo(), e);
		} finally {
			pm.close();
		}
	}
}
