/**
 * Copyright © 2018 Nelson Tavares de Sousa (tavaresdesousa@email.uni-kiel.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.gerdiproject.store;

import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.staticFiles;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.apache.commons.collections.map.HashedMap;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jwt.profile.JwtProfile;
import org.pac4j.sparkjava.SecurityFilter;
import org.pac4j.sparkjava.SparkWebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import de.gerdiproject.store.datamodel.CacheElement;
import de.gerdiproject.store.datamodel.ICredentials;
import de.gerdiproject.store.datamodel.ListElement;
import de.gerdiproject.store.datamodel.Options;
import de.gerdiproject.store.datamodel.ResearchDataInputStream;
import de.gerdiproject.store.datamodel.StoreTask;
import de.gerdiproject.store.handler.PostRootRoute;
import de.gerdiproject.store.pac4j.GerdiConfigFactory;
import de.gerdiproject.store.util.CacheGarbageCollectionTask;
import de.gerdiproject.store.util.ScalingStrategy.Max4Scaler;
import de.gerdiproject.store.util.ScalingStrategy.Max4TaskForOne;
import de.gerdiproject.store.util.ScalingStrategy.OneForAll;
import de.gerdiproject.store.util.ScalingStrategy.OneForOne;
import de.gerdiproject.store.util.ScalingStrategy.ScalingStrategy;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.apis.ExtensionsV1beta1Api;
import io.kubernetes.client.models.ExtensionsV1beta1Deployment;
import io.kubernetes.client.models.ExtensionsV1beta1DeploymentSpec;
import io.kubernetes.client.models.V1Container;
import io.kubernetes.client.models.V1ContainerPort;
import io.kubernetes.client.models.V1DeleteOptions;
import io.kubernetes.client.models.V1ObjectMeta;
import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.models.V1PodList;
import io.kubernetes.client.models.V1PodSpec;
import io.kubernetes.client.models.V1PodTemplateSpec;
import io.kubernetes.client.models.V1VolumeMount;
import io.kubernetes.client.models.V1beta1ReplicaSet;
import io.kubernetes.client.models.V1beta1ReplicaSetList;
import spark.Request;
import spark.Response;

/**
 * This class provides some functionality to implement a store service. It also
 * deals with some API issues, so an implementation only needs to implement the
 * access to the storage.
 *
 * @author Nelson Tavares de Sousa
 *
 * @param <E> The type used to store the credentials. Must implement the
 *            ICredentials interface.
 */
public abstract class AbstractStoreService<E extends ICredentials> {

	/**
	 * This class's logger instance
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStoreService.class);

	private final Options options;

	/**
	 * A map used as a cache
	 */
	private final Map<String, CacheElement<E>> cacheMap = new ConcurrentHashMap<>(); // Possible
																						// to
																						// abstract
																						// here;
																						// allows
																						// for
																						// different
																						// implementations

	// remeber Session and Pods
	private final Map<String, String[]> podCopySrvTaks = new ConcurrentHashMap<>();
	/**
	 * Timer for garbage collection on the cache
	 */
	private final Timer timer = new Timer(true);
	/**
	 * Flag to check if this service was already executed
	 */
	private boolean running = false;
	public static final String NAMESPACE = "default";

	private ScalingStrategy scalingStratgy = new Max4Scaler();

	private CoreV1Api k8sApi;

	/**
	 * 
	 * @param options extends all the imagename, deploymentname, gson builder for
	 *                de/serialize the Credential and for JupiterJub the VolumePaths
	 * @throws IOException  from K8sClient
	 * @throws ApiException from ApiClient
	 */
	protected AbstractStoreService(Options options) throws IOException, ApiException {

		// Run a garbage collection task every 5 minutes
		timer.schedule(new CacheGarbageCollectionTask<E>(this.cacheMap), 300000, 300000);
		ApiClient k8sClient = io.kubernetes.client.util.Config.defaultClient();
		io.kubernetes.client.Configuration.setDefaultApiClient(k8sClient);
		k8sApi = new CoreV1Api();
		this.options = options;
		killAllCopySrvDeployment();

	}

	/**
	 * This method checks whether or not the user is logged in.
	 *
	 * @param creds The stored credentials, may be null if no credentials were
	 *              stored
	 * @return true if logged in, false otherwise
	 */
	protected abstract boolean isLoggedIn(E creds);

	/**
	 * This method is called if a login request is performed.
	 *
	 * @param req A instance of SparkJava's Request class
	 * @param res A instance of SparkJava's Response class
	 * @return An instance of a newly created credential.
	 */
	protected abstract E login(String userId, Request req, Response res);

	/**
	 * Returns the list of files for a requested directory.
	 *
	 * @param directory The requested directory relative to the root directory.
	 * @param creds     The stored credentials, may be null if no credentials were
	 *                  stored
	 * @return A list of elements depicting the files/subdirectories
	 */
	protected abstract List<ListElement> listFiles(String directory, E creds);

	/**
	 * This method is executed once before any copy is triggered. May be overwritten
	 * to perform tasks before starting to copy data sets.
	 *
	 * @param creds The stored credentials, may be null if no credentials were
	 *              stored
	 */
	protected void preCopy(final E creds) { // NOPMD empty on purpose
	}

	/**
	 * This method is executed if a new folder is requested.
	 *
	 * @param dir     The directory where the new folder is requested to be created
	 * @param dirName The name of the requested folder
	 * @param creds   The stored credentials, may be null if no credentials were
	 *                stored
	 * @return true on success, false otherwise
	 */
	protected abstract boolean createDir(final String dir, final String dirName, final E creds);

	/**
	 * Sets the folder in classpath serving static files. Observe: this method must
	 * be called before all other methods.
	 *
	 * @param folder the folder in classpath.
	 */
	protected void registerStaticFolder(final String folder) {
		if (running) {
			throw new IllegalStateException("This method must be called before executing the run method.");
		}
		staticFiles.location(folder);
	}

	/**
	 * This method starts the webserver and initializes all predefined routes. It
	 * must be executed after the inialization of this class.
	 */
	protected void run() {
		if (running) {
			throw new IllegalStateException("The run method may only be executed once.");
		}
		this.running = true;

		//Build the security filter
		 final Config config = new GerdiConfigFactory().build();
		 final SecurityFilter secFilter = new SecurityFilter(config,
		 "DirectBearerAuthClient");

		port(5678);
		// Ignore trailing slashes and add security check for JWT
		before((req, res) -> {
			secFilter.handle(req, res);
		});

		// Accepts new storing tasks and initializes them in the in-memory cache
		post("/", new PostRootRoute<E>(cacheMap));

		// Checker whether or not the user is logged in
		get("/loggedIn/:" + StoreConstants.SESSION_ID, this::getLoggedIn);

		// Return a list with the progress of each element
		get("/progress/:" + StoreConstants.SESSION_ID, this::getProgress);

		// Log in the user
		post("/login/:" + StoreConstants.SESSION_ID, this::postLogin);

		// Start the copy progress
		get("/copy/:" + StoreConstants.SESSION_ID, this::getCopy);

		// Returns a list of files for a given directory
		get("/files/:" + StoreConstants.SESSION_ID, this::getFiles);

		// Create new dir
		get("/createdir/:" + StoreConstants.SESSION_ID + "/:dirname", this::getCreatedir);

		//abort the copyprocess and kill the killCopySrvs for this session
		get("/kill/:" + StoreConstants.SESSION_ID, this::getKill);
		// Returns the changed Strategy
		get("/strategy/:" + "strategyNum", this::getStrategy);
	}

	private Object getStrategy(Request request, Response response) {
		int number = Integer.parseInt(request.params("strategyNum"));
		String stratey = "";
		switch (number) {
		case 1:
			setScalingStrategy(new Max4Scaler());
			stratey = "Max4Scaler";
			break;
		case 2:
			setScalingStrategy(new Max4TaskForOne());
			stratey = "Max4TaskForOne";
			break;
		case 3:
			setScalingStrategy(new OneForAll());
			stratey = "OneForAll";
			break;
		case 4:
			setScalingStrategy(new OneForOne());
			stratey = "OneForOne";
			break;
		default:
			setScalingStrategy(new Max4Scaler());
			stratey = "Max4Scaler";
			break;
		}

		return "Strategy: " + stratey;
	}

	private Object getCreatedir(Request request, Response response) {
		final E creds = cacheMap.get(request.params(StoreConstants.SESSION_ID)).getCredentials();
		if (creds == null) {
			response.status(403);
			return "Not logged in";
		}
		final String dirName = request.params("dirname");
		final String dir = request.queryParamOrDefault(StoreConstants.DIR_QUERYPARAM, "/");
		final boolean created = createDir(dir, dirName, creds);
		return String.format(StoreConstants.DIR_CREATED_RESPONSE, created);
	}

	private Object getFiles(Request request, Response response) {
		final E creds = cacheMap.get(request.params(StoreConstants.SESSION_ID)).getCredentials();
		if (creds == null) {
			response.status(403);
			return "Not logged in";
		}
		final String dir = request.queryParamOrDefault(StoreConstants.DIR_QUERYPARAM, "/");
		final List<ListElement> ret = listFiles(dir, creds);
		return new Gson().toJson(ret);
	}

	private Object getCopy(Request request, Response response) throws ExecutionException {
		//TODO: nach der evaluation die prinln entfernen
		//long start= System.currentTimeMillis();
		final String session = request.params(StoreConstants.SESSION_ID);
		final CacheElement<E> cacheElement = cacheMap.get(session);
		ExtensionsV1beta1Api extensionV1Api = new ExtensionsV1beta1Api();
		extensionV1Api.setApiClient(k8sApi.getApiClient());
		final StoreTask task = cacheElement.getTask();

		// Don't start the copy process twice
		if (task.isStarted()) {
			return "Process already started";
		} else {
			task.setStarted(true);
		}

		final E creds = cacheElement.getCredentials();
		final String targetDir = request.queryParamOrDefault(StoreConstants.DIR_QUERYPARAM, "/");
		this.preCopy(creds);

		int replicas = replicationChooser(cacheElement.getTask().getElements().size());
		ExtensionsV1beta1Deployment deploymentStart = null;

		// deployment wird der Session zugeordnet
		try {
			deploymentStart = createCopySrvDeployment(options.getCopySrvDeploymentName() + session, replicas);
		} catch (ApiException e1) {
			LOGGER.error(e1.getResponseBody(), e1);
			response.status(409); // Error
			e1.printStackTrace();
			return " Deployment"+options.getCopySrvDeploymentName() + session+" konnte nicht erstellt werden";
		}
		if (null == deploymentStart) {
			System.out.println("hier stimmt was nicht mit dem deployment");
		}

		// damit der Thread damit arbeiten kann
		final ExtensionsV1beta1Deployment deployment = deploymentStart;
		new Thread(() -> {
			V1PodList pods = null;
			try {
				boolean killProgress = false;
				Integer readyReplicas = null;
				do {
					Thread.sleep(1000 / 2);
					readyReplicas = extensionV1Api
							.readNamespacedDeploymentStatus(deployment.getMetadata().getName(), NAMESPACE, null)
							.getStatus().getReadyReplicas();
					if (readyReplicas == null)
						readyReplicas = 0;

				} while (readyReplicas < replicas);

				//System.out.println("Start getcopy: "+start+"\n Pods fertig: "+System.currentTimeMillis());
				pods = k8sApi.listNamespacedPod(NAMESPACE, true, null, null, null,
						"app=" + deployment.getMetadata().getName(), null, null, null, null);

				String[] podIP = new String[pods.getItems().size()];
				int count = 0;
				for (V1Pod pod : pods.getItems()) {
					podIP[count++] = pod.getStatus().getPodIP(); // jetzt habe ich die IPs der einezenen Pods
				}

				podCopySrvTaks.put(session, podIP);
				
				List<String> inputStreamUrl = new ArrayList<String>();
				for (ResearchDataInputStream inputStream : cacheElement.getTask().getElements()) {
					inputStreamUrl.add(inputStream.getUrl().toString());
				}
				final int podSize = podIP.length;
				final int elementsize = inputStreamUrl.size();

				// hier werden die Aufgaben verteilt
				for (int i = 0; i < podSize; i++) {
					List<String> sublist = new ArrayList<String>();
					for (int j = i; j < elementsize; j += podSize) {
						sublist.add(inputStreamUrl.get(j));
					}
					// so stelle ich sicher ob der dienst bereit ist
					while (taskDoneCall(podIP[i])) {
						Thread.sleep(1000 / 4);
					}
					boolean result = copyFile(creds, targetDir, sublist, podIP[i]);
					if (!result) {
						killCopySrvDeployment(deployment.getMetadata().getName());
						podCopySrvTaks.remove(session);
						killProgress = true;
					}
				}
				// hier wird der CopySrv vernichtet: erst das deployment,die Pods und dann der
				// map eintrag

				// Warteschleife bis die Pods fertig sind und dann werden sie vernichtet
				if (!killProgress) {
					for (V1Pod pod : pods.getItems()) {
						while (!taskDoneCall(pod.getStatus().getPodIP())) {
							Thread.sleep(1000 * 2);
						}
					}
					killCopySrvDeployment(deployment.getMetadata().getName());
					podCopySrvTaks.remove(session);
				}
			} catch (ApiException | InterruptedException e) {
				LOGGER.error(e.toString());
				response.status(409); // Error
				e.printStackTrace();
			}
		}).start();
		return "";
	}

	/**
	 * 
	 * @param podIP
	 * @return if the getTaskDone Call answer 200 the methode return false if the
	 *         service is unavaiable or the task is done the method return true
	 */
	private boolean taskDoneCall(String podIP) {
		boolean value = true;
		try {
			HttpURLConnection con;
			con = (HttpURLConnection) new URL("http", podIP, StoreConstants.COPYSRV_CONTAINERPORT, "/taskDone")
					.openConnection();
			con.setRequestMethod("GET");
			value = con.getResponseCode() != HttpURLConnection.HTTP_OK;
		} catch (IOException e) {
			//e.printStackTrace();
		}
		return value;
	}

	/**
	 * send the inputStreamUrl via Post-request to the copySrv and ignore the
	 * http-response the getCopy controll the Downloadstatus and kill the Services
	 * if they finish or fail
	 * 
	 * @param creds
	 * @param targetDir
	 * @param inputStreamUrl
	 * @param hostIP
	 * @return
	 */
	protected boolean copyFile(final E creds, final String targetDir, final List<String> inputStreamUrl,
			final String hostIP) {
		final URL url;
		final Map<String, Object> send = new HashMap<>();
		send.put("cred", creds);
		send.put("targetDir", targetDir);
		send.put("inputStreamUrl", inputStreamUrl);

		try {
			String copySrvIP = hostIP;
			int port = StoreConstants.COPYSRV_CONTAINERPORT;
			url = new URL("http", copySrvIP, port, "/copy");
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			con.setDoInput(true);
			con.setDoOutput(true);
			// body
			String json = options.getCredentialSerialzer().toJson(send);
			// da sende ich den body
			try (OutputStream output = con.getOutputStream()) {
				output.write(json.getBytes("utf-8"));
				
				output.flush();
				output.close(); // gibt die ressourcen wieder frei
				new Thread(() -> {
					try {
						con.getResponseCode();
						con.disconnect();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}}).start();
			}
			
		} catch (ProtocolException | MalformedURLException e1) {
			e1.printStackTrace();
			LOGGER.error("Error while copying files.", e1);
			return false;
		} catch (IOException | NullPointerException e) {
			LOGGER.error("Error while copying files.", e);
			return false;
		}
		return true;
	}

	/**
	 * @param deploymentName
	 * @param numberOfReplicas
	 * @return ExtensionsV1beta1Deployment
	 * @throws ApiException
	 */
	private ExtensionsV1beta1Deployment createCopySrvDeployment(String deploymentName, int numberOfReplicas)
			throws ApiException {
		ExtensionsV1beta1Api extensionV1Api = new ExtensionsV1beta1Api();
		extensionV1Api.setApiClient(k8sApi.getApiClient());
		ExtensionsV1beta1Deployment result = null;
		// hier wird das Deployment wie in der YAML definiert
		ExtensionsV1beta1Deployment body = new ExtensionsV1beta1Deployment();
		V1ObjectMeta metadata = new V1ObjectMeta();
		metadata.name(deploymentName);
		body.apiVersion("extensions/v1beta1"); // wie das YAML-Format gestaltetist
		body.kind("Deployment");
		body.setMetadata(metadata);
		// Spec

		ExtensionsV1beta1DeploymentSpec spec = new ExtensionsV1beta1DeploymentSpec();
		// zu beginn soll es eine INstanz geben
		spec.setReplicas(numberOfReplicas);
		V1PodTemplateSpec template = new V1PodTemplateSpec();
		V1ObjectMeta metadataTemplateSpec = new V1ObjectMeta();
		// template vom spec dort wird auch das ContainerImage angegeben
		metadataTemplateSpec.name(deploymentName);
		Map<String, String> labelsMetadataTemplateSpec = new HashedMap();
		labelsMetadataTemplateSpec.put("app", deploymentName);
		labelsMetadataTemplateSpec.put("copySrv", options.getCopySrvDeploymentName());
		metadataTemplateSpec.setLabels(labelsMetadataTemplateSpec);
		template.setMetadata(metadataTemplateSpec);
		V1PodSpec specTemplateSpec = new V1PodSpec();
		V1Container containersItem = new V1Container();
		containersItem.name(deploymentName);
		containersItem.setImage(options.getCopySrvImageName());

		// JupyterHubCopySrv braucht die Pfade von dem Persisten VolumeClaim zum
		// Speichern
		if (options.getVolume() != null && options.getVolumeMount() != null) {
			// volume Mount im Container
			List<V1VolumeMount> volumeMounts = new ArrayList<V1VolumeMount>();
			volumeMounts.add(options.getVolumeMount());
			containersItem.volumeMounts(volumeMounts);
			// das Volume in der spec
			specTemplateSpec.addVolumesItem(options.getVolume());
			
		}
		template.setSpec(specTemplateSpec);
		V1ContainerPort portsItem = new V1ContainerPort();
		portsItem.setContainerPort(StoreConstants.COPYSRV_CONTAINERPORT);
		containersItem.addPortsItem(portsItem);
		specTemplateSpec.addContainersItem(containersItem);

		spec.setTemplate(template);
		body.setSpec(spec);
		
		result = extensionV1Api.createNamespacedDeployment(NAMESPACE, body, null, null, null);
		if(result==null) {
			LOGGER.error("der Body des Fehlerhaften Deployments: " +body.toString());
		}
		return result;
	}

	private Object getKill(Request request, Response response) {
		String output="";
		try {
			String session = request.params(StoreConstants.SESSION_ID);
			if (null == podCopySrvTaks.get(session)) {
				response.status(404);
				output= "Session does not exist.";
			}
			output=killCopySrvDeployment(options.getCopySrvDeploymentName() + session);
			podCopySrvTaks.remove(session);
		} catch (ApiException e) {
			response.status(409); // Error
			LOGGER.error(e.getResponseBody(), e);
			e.printStackTrace();
		}
		return output;
	}

	
	private String killAllCopySrvDeployment() throws ApiException {
		ExtensionsV1beta1Api extensionV1Api = new ExtensionsV1beta1Api();
		extensionV1Api.setApiClient(k8sApi.getApiClient());

		V1beta1ReplicaSetList rs = extensionV1Api.listNamespacedReplicaSet(NAMESPACE, true, null, null, null,
				"copySrv=" + options.getCopySrvDeploymentName(), null, null, null, null);
		extensionV1Api.deleteCollectionNamespacedDeployment(NAMESPACE, true, null, null, null,"copySrv=" + options.getCopySrvDeploymentName(),
				null, null, null, null);
		for (V1beta1ReplicaSet r : rs.getItems()) {
			com.squareup.okhttp.Response response;
			try {
				response = extensionV1Api.deleteNamespacedReplicaSetCall(r.getMetadata().getName(), NAMESPACE, null,
						new V1DeleteOptions().gracePeriodSeconds(0L).propagationPolicy("Foreground"), null, null, null,
						null, null, null).execute();
				if (!response.isSuccessful()) {
					LOGGER.warn("Couldn't delete rs [{}] with reason: {}", r.getMetadata().getName(),
							response.message());
					return "Couldn't delete rs [{}] with reason: {}"+ r.getMetadata().getName()+
							response.message();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return "Alle Services wurden beendet";
	}
		
	
	/**
	 * kill the CopySrvDeployment
	 * 
	 * @return V1Status
	 * @throws ApiException
	 */
	private String  killCopySrvDeployment(String deploymentName) throws ApiException {
		ExtensionsV1beta1Api extensionV1Api = new ExtensionsV1beta1Api();
		extensionV1Api.setApiClient(k8sApi.getApiClient());

		V1beta1ReplicaSetList rs = extensionV1Api.listNamespacedReplicaSet(NAMESPACE, true, null, null, null,
				"app=" + deploymentName, null, null, null, null);
		extensionV1Api.deleteCollectionNamespacedDeployment(NAMESPACE, true, null, null, null, "app=" + deploymentName,
				null, null, null, null);
		for (V1beta1ReplicaSet r : rs.getItems()) {
			com.squareup.okhttp.Response response;
			try {
				response = extensionV1Api.deleteNamespacedReplicaSetCall(r.getMetadata().getName(), NAMESPACE, null,
						new V1DeleteOptions().gracePeriodSeconds(0L).propagationPolicy("Foreground"), null, null, null,
						null, null, null).execute();
				if (!response.isSuccessful()) {
					LOGGER.warn("Couldn't delete rs [{}] with reason: {}", r.getMetadata().getName(),
							response.message());
					return "Couldn't delete rs [{}] with reason: {}"+ r.getMetadata().getName()+
							response.message();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return "service beendnet";
	}

	private int replicationChooser(int numberOfTaskToCopy) {
		return scalingStratgy.replicaChoice(numberOfTaskToCopy);
	}

	/**
	 * if is not set, the standard is Max4Scaler
	 * 
	 * set antother ScalingStategy form de.gerdiproject.store.util
	 */
	protected void setScalingStrategy(ScalingStrategy scalingStrategy) {
		this.scalingStratgy = scalingStrategy;
	}

	protected ScalingStrategy getScalingStrategy() {
		return scalingStratgy;
	}

	private Object postLogin(Request request, Response response) {
		final CacheElement<E> elem = cacheMap.get(request.params(StoreConstants.SESSION_ID));
		if (elem == null) {
			response.status(404);
			return "Session does not exist.";
		}
		final E credentials = this.login(getUserProfile(request, response).getAttribute("preferred_username").toString(), request, response);
		//final E credentials = this.login("oppicool", request, response);
		if (credentials == null) {
			if (LOGGER.isWarnEnabled()) {
				LOGGER.warn("Login failed for Session " + request.queryParams(StoreConstants.SESSION_ID));
			}
			return "Login failed";
		} else {
			elem.setCredentials(credentials);
			return "Login Successful";
		}
	}

	private Object getProgress(Request request, Response response) {
		String session = request.params(StoreConstants.SESSION_ID);
		CacheElement<E> cacheElement = cacheMap.get(session);

		if (cacheElement == null) {
			response.status(404);
			return "Session does not exist.";
		}

		if (podCopySrvTaks.get(session) == null) {
			response.status(404);
			return "The progress does not exist!";
		}
		String[] podsIP = podCopySrvTaks.get(session);
		/*
		 * hier bekomme ich meine Serialisierten RersearchdataInputstream und schreibe
		 * siehe in mein Gedaechnis podProgressMessage falls ein Pod beendet wurde wird
		 * kein neuer Progress gespeichert
		 */
		StringBuffer progressList = new StringBuffer();
		progressList.append("[");
		try {
			// aus der Map baue ich dann neue ProgressListen
			// indem ich die Klammer am Ende entferne und mit , konkatiniere

			for (String pod : podsIP) {
				int port = StoreConstants.COPYSRV_CONTAINERPORT;
				HttpURLConnection con;
				con = (HttpURLConnection) new URL("http", pod, port, "/getProgress").openConnection();
				con.setRequestMethod("GET");
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				String inputLine;
				StringBuffer jsonResponse = new StringBuffer();
				while ((inputLine = in.readLine()) != null) {
					jsonResponse.append(inputLine);
				}
				in.close();
				con.disconnect();
				progressList.append(jsonResponse.substring(1, jsonResponse.length() - 1));
				progressList.append(", ");
			}
			progressList.delete((progressList.length() - 2), progressList.length());
		} catch (IOException e) {
			e.printStackTrace();
			//es kann sein das zu Anfang noch nicht alle CopySrv fertig intialsiert sind
			//und so lange wird die Leere Liste returnt
			progressList = new StringBuffer();
			progressList.append("[");
		}
		return progressList.append("]");
	}

	private Object getLoggedIn(Request request, Response response) {
		final CacheElement<E> element = cacheMap.get(request.params(StoreConstants.SESSION_ID));
		if (element == null) {
			response.status(404);
			if (LOGGER.isWarnEnabled()) {
				LOGGER.warn("Attempt to access non-existent Session " + request.params(StoreConstants.SESSION_ID));
			}
			return "Session ID does not exist";
		}
		final boolean loggedIn = this.isLoggedIn(element.getCredentials());
		return String.format(StoreConstants.IS_LOGGED_IN_RESPONSE, loggedIn);
	}

	private final JwtProfile getUserProfile(Request req, Response res) {
		WebContext context = new SparkWebContext(req, res);
		ProfileManager manager = new ProfileManager(context);
		Optional<CommonProfile> profile = manager.get(true);
		JwtProfile jwtProfile = (JwtProfile) profile.get();
		return jwtProfile;
	}
}
