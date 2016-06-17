/**
 * Copyright 2015 Jan Lolling jan.lolling@gmail.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.jlo.talendcomp.youtubeanalytics;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Clock;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.youtubeAnalytics.YouTubeAnalytics;
import com.google.api.services.youtubeAnalytics.YouTubeAnalyticsScopes;
import com.google.api.services.youtubeAnalytics.model.ResultTable;
import com.google.api.services.youtubeAnalytics.model.ResultTable.ColumnHeaders;

public class YoutubeAnalyticsInput {

	private static final Map<String, YoutubeAnalyticsInput> clientCache = new HashMap<String, YoutubeAnalyticsInput>();
	private final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
	private final JsonFactory JSON_FACTORY = new JacksonFactory();
	private String clientSecretFile = null;
	private String accountEmail;
	private String channelId = null;
	private String startDate = null;
	private String endDate = null;
	private String metrics = null;
	private String dimensions = null;
	private String sorts = null;
	private String filters = null;
	private String applicationName = null;
	// default date format for the API
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	private int maxRows = 0;
	private int timeoutInSeconds = 120;
	private int lastFetchedRowCount = 0;
	private int overallRowCount = 0;
	private int currentIndex = 0;
	private int startIndex = 1;
	private long timeMillisOffsetToPast = 10000;
	/** Global instance of YoutubeAnalytics object to make analytic API requests. */
	private YouTubeAnalytics analytics;
	private com.google.api.services.youtubeAnalytics.YouTubeAnalytics.Reports.Query query;
	private ResultTable resultTable = null;
	private List<String> requestedColumnNames = new ArrayList<String>();
	private List<List<Object>> lastResultSet;
	private String credentialDataStoreDir = null;
	private boolean debug = false;
	
	public static void putIntoCache(String key, YoutubeAnalyticsInput yai) {
		clientCache.put(key, yai);
	}
	
	public static YoutubeAnalyticsInput getFromCache(String key) {
		return clientCache.get(key);
	}

	public void setTimeoutInSeconds(int timeoutInSeconds) {
		this.timeoutInSeconds = timeoutInSeconds;
	}

	public void setStartDate(String yyyy_MM_dd) {
		this.startDate = yyyy_MM_dd;
	}

	public void setStartDate(Date startDate) {
		this.startDate = sdf.format(startDate);
	}

	/**
	 * for selecting data for one day: set start date == end date
	 * 
	 * @param yyyyMMdd
	 */
	public void setEndDate(String yyyy_MM_dd) {
		this.endDate = yyyy_MM_dd;
	}

	/**
	 * for selecting data for one day: set start date == end date
	 * 
	 * @param yyyyMMdd
	 */
	public void setEndDate(Date endDate) {
		this.endDate = sdf.format(endDate);
	}

	public void setMetrics(String metrics) {
		this.metrics = metrics;
	}

	public void setDimensions(String dimensions) {
		this.dimensions = dimensions;
	}

	public void setSorts(String sorts) {
		this.sorts = sorts;
	}

	/**
	 * use operators like:
	 * == exact match
	 * =@ contains
	 * =~ matches regular expression
	 * != does not contains
	 * separate with , for OR and ; for AND
	 * @param filters
	 */
	public void setFilters(String filters) {
		this.filters = filters;
	}

	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	/**
	 * Authorizes the installed application to access user's protected YouTube
	 * data.
	 * 
	 * @param scopes
	 *            list of scopes needed to access general and analytic YouTube
	 *            info.
	 */
	private Credential authorizeWithClientSecret() throws Exception {
		if (clientSecretFile == null) {
			throw new IllegalStateException("client secret file is not set");
		}
		File secretFile = new File(clientSecretFile);
		if (secretFile.exists() == false) {
			throw new Exception("Client secret file:" + secretFile.getAbsolutePath() + " does not exists or is not readable.");
		}
		Reader reader = new FileReader(secretFile);
		// Load client secrets.
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, reader);
		try {
			reader.close();
		} catch (Throwable e) {}
		// Checks that the defaults have been replaced (Default =
		// "Enter X here").
		if (clientSecrets.getDetails().getClientId().startsWith("Enter")
				|| clientSecrets.getDetails().getClientSecret()
						.startsWith("Enter ")) {
			throw new Exception("The client secret file does not contains the credentials!");
		}
		credentialDataStoreDir = secretFile.getParent() + "/" + clientSecrets.getDetails().getClientId() + "/";
		File credentialDataStoreDirFile = new File(credentialDataStoreDir);             
		if (credentialDataStoreDirFile.exists() == false && credentialDataStoreDirFile.mkdirs() == false) {
			throw new Exception("Credentedial data dir does not exists or cannot created:" + credentialDataStoreDir);
		}
		if (debug) {
			System.out.println("Credential data store dir:" + credentialDataStoreDir);
		}
		FileDataStoreFactory fdsf = new FileDataStoreFactory(credentialDataStoreDirFile);
		// Set up authorization code flow.
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
					HTTP_TRANSPORT, 
					JSON_FACTORY, 
					clientSecrets, 
					Arrays.asList(YouTubeAnalyticsScopes.YT_ANALYTICS_READONLY))
				.setDataStoreFactory(fdsf)
				.setClock(new Clock() {
					@Override
					public long currentTimeMillis() {
						// we must be sure, that we are always in the past from Googles point of view
						// otherwise we get an "invalid_grant" error
						return System.currentTimeMillis() - timeMillisOffsetToPast;
					}
				})
				.build();
		// Authorize.
		return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize(accountEmail);
	}

	public void initializeAnalyticsClient() throws Exception {
		// Authorization.
		final Credential credential = authorizeWithClientSecret();
		// YouTube object used to do all analytic API requests.
		analytics = new YouTubeAnalytics.Builder(
				  HTTP_TRANSPORT, 
				  JSON_FACTORY, 
				  credential)
			.setHttpRequestInitializer(
				 new HttpRequestInitializer() {
					 @Override
					 public void initialize(final HttpRequest httpRequest) throws IOException {
						 credential.initialize(httpRequest);
						 httpRequest.setConnectTimeout(timeoutInSeconds * 1000);
						 httpRequest.setReadTimeout(timeoutInSeconds * 1000);
					 }
				 })
		    .setApplicationName(applicationName)
		    .build();
	}

	public void executeQuery() throws Exception {
		executeDataQuery();
		checkColumns();
	}

	private void executeDataQuery() throws Exception {
		query = analytics.reports()
				.query("channel==" + channelId,  
					startDate,
					endDate,
					metrics);
		if (filters != null && filters.trim().isEmpty() == false) {
			query.setFilters(filters);
		}
		if (dimensions != null && dimensions.trim().isEmpty() == false) {
			query.setDimensions(dimensions);
		}
		if (sorts != null && sorts.trim().isEmpty() == false) {
			query.setSort(sorts);
		}
		if (maxRows > 0) {
			query.setMaxResults(maxRows);
		}
		resultTable = query.execute();
		requestedColumnNames = new ArrayList<String>(); // reset
		addRequestedColumns(dimensions); // must added at first!
		addRequestedColumns(metrics);
		lastResultSet = resultTable.getRows();
		if (lastResultSet == null) {
			// fake an empty result set to avoid breaking further processing
			lastResultSet = new ArrayList<List<Object>>();
		}
		lastFetchedRowCount = lastResultSet.size();
		currentIndex = 0;
		overallRowCount = 0;
		startIndex = 1;
		if (debug) {
			System.out.println("Result set column headers:");
			for (ColumnHeaders h : getColumnHeaders()) {
				System.out.println("name: " + h.getName() + " columnType: " + h.getColumnType() + " dataType: " + h.getDataType());
			}
			System.out.println("Result set initially contains " + lastFetchedRowCount + " rows.");
		}
	}

	/**
	 * checks if more result set available
	 * @return true if more data sets available
	 * @throws Exception
	 */
	public boolean hasNextDataset() throws Exception {
		if (query == null) {
			throw new IllegalStateException("No query executed before");
		}
		// check if are at the end of previously fetched data
		// we need fetch data if
		// current index reached the max of last fetched data
		// and count of last fetched data == maxResult what indicated that there
		// is more than we have currently fetched
		if (currentIndex == lastFetchedRowCount
				&& (maxRows > 0 && lastFetchedRowCount == maxRows)) {
			startIndex = startIndex + lastFetchedRowCount;
			query.setStartIndex(startIndex);
			if (maxRows > 0) {
				query.setMaxResults(maxRows);
			}
			resultTable = query.execute();
			lastResultSet = resultTable.getRows();
			if (lastResultSet == null) {
				lastResultSet = new ArrayList<List<Object>>();
			}
			lastFetchedRowCount = lastResultSet.size();
			currentIndex = 0;
			if (debug) {
				System.out.println("Result set start with index " + startIndex + " contains " + lastFetchedRowCount + " rows.");
			}
		}
		if (lastFetchedRowCount > 0 && currentIndex < lastFetchedRowCount) {
			return true;
		} else {
			return false;
		}
	}
	
	public List<Object> getNextDataset() {
		if (query == null) {
			throw new IllegalStateException("No query executed before");
		}
		overallRowCount++;
		return lastResultSet.get(currentIndex++);
	}

	private void addRequestedColumns(String columnStr) {
		if (columnStr != null) {
			StringTokenizer st = new StringTokenizer(columnStr, ",");
			while (st.hasMoreElements()) {
				requestedColumnNames.add(st.nextToken());
			}
		}
	}

	private void checkColumns() throws Exception {
		List<String> columnsFromData = getColumnNames();
		if (columnsFromData.size() != requestedColumnNames.size()) {
			throw new Exception("Requested column names="
					+ requestedColumnNames.size() + " columnsFromData="
					+ columnsFromData.size());
		} else {
			for (int i = 0; i < columnsFromData.size(); i++) {
				String colNameFromData = columnsFromData.get(i);
				String colNameFromRequest = requestedColumnNames.get(i);
				if (colNameFromData.equalsIgnoreCase(colNameFromRequest) == false) {
					throw new Exception("At position:" + i
							+ " column missmatch: colNameFromRequest="
							+ colNameFromRequest + " colNameFromData="
							+ colNameFromData);
				}
			}
		}
	}
	
	public List<ColumnHeaders> getColumnHeaders() {
		if (resultTable != null) {
			return resultTable.getColumnHeaders();
		} else {
			throw new IllegalStateException("ResultTable is null, no query executed.");
		}
	}

	public List<String> getColumnNames() {
		List<ColumnHeaders> listHeaders = resultTable.getColumnHeaders();
		List<String> names = new ArrayList<String>();
		for (ColumnHeaders ch : listHeaders) {
			names.add(ch.getName());
		}
		return names;
	}

	public List<String> getColumnTypes() {
		List<ColumnHeaders> listHeaders = resultTable.getColumnHeaders();
		List<String> types = new ArrayList<String>();
		for (ColumnHeaders ch : listHeaders) {
			types.add(ch.getDataType());
		}
		return types;
	}

	public void setClientSecretFile(String clientSecretFile) {
		this.clientSecretFile = clientSecretFile;
	}

	public int getOverAllCountRows() {
		return overallRowCount;
	}

	public void setTimeOffsetMillisToPast(long timeMillisOffsetToPast) {
		this.timeMillisOffsetToPast = timeMillisOffsetToPast;
	}

	public void setMaxRows(int fetchSize) {
		this.maxRows = fetchSize;
	}

	public String getCredentialDataStoreDir() {
		return credentialDataStoreDir;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public String getAccountEmail() {
		return accountEmail;
	}

	public void setAccountEmail(String accountEmail) {
		this.accountEmail = accountEmail;
	}
	
}
