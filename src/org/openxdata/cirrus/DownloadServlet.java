package org.openxdata.cirrus;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fcitmuk.epihandy.StudyDef;
import org.fcitmuk.epihandy.StudyDefList;

import com.jcraft.jzlib.JZlib;
import com.jcraft.jzlib.ZOutputStream;

@SuppressWarnings("serial")
public class DownloadServlet extends HttpServlet {

	private static final Logger log = Logger.getLogger(DownloadServlet.class
			.getName());

	public static final byte ACTION_DOWNLOAD_USERS = 7;
	public static final byte ACTION_DOWNLOAD_USERS_AND_FORMS = 11;
	public static final byte ACTION_DOWNLOAD_STUDY_LIST = 2;
	public static final byte STATUS_SUCCESS = 1;

	@SuppressWarnings({ "unchecked" })
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		String studyPath = req.getPathInfo();

		if (studyPath == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
					"Must specify study path.");
		}

		InputStream in = req.getInputStream();
		OutputStream out = resp.getOutputStream();

		ZOutputStream zOut = null;

		DataInputStream dataIn = null;
		DataOutputStream dataOut = null;

		resp.setContentType("application/octet-stream");

		log.info("Creating streams");

		zOut = new ZOutputStream(out, JZlib.Z_BEST_COMPRESSION);
		dataIn = new DataInputStream(in);
		dataOut = new DataOutputStream(zOut);

		log.info("Starting persistence manager");
		PersistenceManager pm = PMF.get().getPersistenceManager();

		log.info("Creating query");
		Query query = pm.newQuery(Form.class);
		query.setFilter("path == pathParam");
		query.declareParameters("String pathParam");
		query.setRange(0, 1);

		log.info("Building study");
		StudyDefList studyDefList = new StudyDefList();
		StudyDef studyDef = new StudyDef(1, "A study", "varName");
		studyDefList.addStudy(studyDef);

		log.info("Reading download header from client");
		String username = dataIn.readUTF();
		String password = dataIn.readUTF();
		String serializer = dataIn.readUTF();
		String locale = dataIn.readUTF();
		String action = req.getParameter("action");

		log.info("Header received: username=" + username + ", password="
				+ password + ", serializer=" + serializer + ", locale="
				+ locale + ", action=" + action);

		try {

			log.info("Reading action");
			byte actionByte = dataIn.readByte();

			if (actionByte == ACTION_DOWNLOAD_USERS) {
				log.info("Downloading users");
				dataOut.writeByte(STATUS_SUCCESS);
				writeUserList(dataOut);
			} else if (actionByte == ACTION_DOWNLOAD_STUDY_LIST) {
				log.info("Downloading study list");
				dataOut.writeByte(STATUS_SUCCESS);
				studyDefList.write(dataOut);
			} else if (actionByte == ACTION_DOWNLOAD_USERS_AND_FORMS) {
				log.info("Downloading users and forms");
				dataOut.writeByte(STATUS_SUCCESS);
				writeUserList(dataOut);
				try {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					DataOutputStream mdos = new DataOutputStream(baos);
					mdos.writeInt(studyDef.getId());
					mdos.writeUTF(studyDef.getName());
					mdos.writeUTF(studyDef.getVariableName());
					mdos.writeShort(1);

					List<Form> forms = (List<Form>) query.execute(studyPath);
					if (!forms.isEmpty()) {
						byte[] formContent = forms.get(0).getFormContent()
								.getBytes();
						mdos.write(formContent);
					}

					byte[] studyBytes = baos.toByteArray();
					dataOut.writeInt(studyBytes.length);
					dataOut.write(studyBytes);

				} finally {
					pm.close();
				}
			} else {
				log.warning("Unknown action received. Action=" + actionByte);
			}
		} catch (Exception e) {
			log.log(Level.WARNING,
					"Failed to download form for path " + req.getPathInfo(), e);
		} finally {
			if (dataOut != null)
				dataOut.flush();
			if (zOut != null)
				zOut.finish();
			resp.flushBuffer();
		}
	}

	private void writeUserList(DataOutputStream dataOut) throws IOException {
		dataOut.writeShort(1);
		dataOut.writeInt(2);
		dataOut.writeUTF("admin");
		dataOut.writeUTF("7357bec928a1af86415f7b8c11245296ec1779d");
		dataOut.writeUTF("e2597cf74095403889c6b07b46d8af5d94b8e6");
	}
}
