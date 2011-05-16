package org.openxdata.cirrus;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class IndexServlet extends HttpServlet {

	@SuppressWarnings("unchecked")
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		PersistenceManager pm = PMF.get().getPersistenceManager();

		try {

			// Remove form if user clicked link
			String removePath = req.getParameter("remove");
			if (removePath != null) {
				Query pathQuery = pm.newQuery(Form.class);
				pathQuery.setFilter("path == pathParam");
				pathQuery.declareParameters("String pathParam");
				List<Form> formsToRemove = (List<Form>) pathQuery
						.execute(removePath);
				if (formsToRemove.isEmpty()) {
					resp.sendError(HttpServletResponse.SC_FOUND,
							"Form for path " + removePath + " not found.");
					return;
				}

				Form formToRemove = formsToRemove.get(0);
				pm.deletePersistent(formToRemove);

				resp.sendRedirect("/");
				return;
			}

			Query query = pm.newQuery(Form.class);
			List<Form> forms = (List<Form>) query.execute();

			resp.setContentType("text/html");

			resp.getWriter().print("<h1>Stored Forms</h1>");

			String uploadMsg = "<a href=\"/upload{0}\">/upload{0}</a>";
			String downloadMsg = "<a href=\"/download{0}\">/download{0}</a>";
			String removeMsg = "<a href=\"/?remove={0}\">Remove</a>";

			if (forms.isEmpty()) {
				String uploadUrl = MessageFormat.format("{0}://{1}/upload{2}",
						req.getScheme(), req.getServerName(), "&lt;path&gt;");
				String downloadUrl = MessageFormat.format(
						"{0}://{1}/download{2}", req.getScheme(),
						req.getServerName(), "&lt;path&gt;");
				resp.getWriter()
						.print("<div style='margin-right: 15px; margin-left: 15px;'\\>"
								+ "There are currently no forms stored. To store a form, "
								+ "post using the form designer prototype to a URL "
								+ "matching the pattern '"
								+ uploadUrl
								+ "'. To download using the mobile client, "
								+ "configure the download URL as '"
								+ downloadUrl + "'.</div>");
				return;
			}

			resp.getWriter()
					.print("<table border=\"1\" cellspacing=\"3\"><tr>"
							+ "<th>Path</th>" + "<th>Upload Link</th>"
							+ "<th>Download Link</th>" + "<th> </th>" + "</tr>");
			for (Form form : forms) {
				String downloadLink = MessageFormat.format(downloadMsg,
						form.getPath());
				String uploadLink = MessageFormat.format(uploadMsg,
						form.getPath());
				String removeLink = MessageFormat.format(removeMsg,
						form.getPath());
				resp.getWriter().print(
						"<tr><td>" + form.getPath() + "</td><td>" + uploadLink
								+ "</td><td>" + downloadLink + "</td><td>"
								+ removeLink + "</td></tr>");
			}
			resp.getWriter().print("</table>");
		} finally {
			pm.close();
		}
	}
}
