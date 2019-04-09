import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;


public class TestYoutube {

	private static Map<String, Object> globalMap = new HashMap<String, Object>();
	private static String jobName = "test";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			test1();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static class row1Struct {
		
		String video;
		BigDecimal views;
		Long likes;
		
		@Override
		public String toString() {
			return "country=" + video + ", views=" + views + ", likes=" + likes;
		}
	}
	
	private static void test1() throws Exception {
		int tos_count_tYoutubeAnalyticsInput_1 = 0;
		row1Struct row1 = null;
		// start creating client
		de.jlo.talendcomp.youtubeanalytics.YoutubeAnalyticsInput tYoutubeAnalyticsInput_1 = de.jlo.talendcomp.youtubeanalytics.YoutubeAnalyticsInput
				.getFromCache("/Data/Talend/testdata/ga/config/client_secret_503880615382-n8ti68l59e04hpuvljrbe6hml9ov5jch.apps.googleusercontent.com.json"
						+ "" + "tYoutubeAnalyticsInput_1" + jobName);
		if (tYoutubeAnalyticsInput_1 == null) {
			tYoutubeAnalyticsInput_1 = new de.jlo.talendcomp.youtubeanalytics.YoutubeAnalyticsInput();
			tYoutubeAnalyticsInput_1.setDebug(true);
			tYoutubeAnalyticsInput_1
					.setApplicationName("Talend Component Development");
			tYoutubeAnalyticsInput_1
					.setAccountEmail("jan.lolling@gmail.com");
			tYoutubeAnalyticsInput_1
					.setClientSecretFile("/Data/Talend/testdata/ga/config/client_secret_503880615382-n8ti68l59e04hpuvljrbe6hml9ov5jch.apps.googleusercontent.com.json");
			tYoutubeAnalyticsInput_1.setTimeoutInSeconds(240);
			tYoutubeAnalyticsInput_1.setTimeOffsetMillisToPast(10000);
			try {
				// initialize client with private key
				tYoutubeAnalyticsInput_1.initializeAnalyticsClient();
			} catch (Exception e) {
				globalMap.put("tYoutubeAnalyticsInput_1_ERROR_MESSAGE",
						e.getMessage());
				throw e;
			}
			globalMap.put("tYoutubeAnalyticsInput_1",
					tYoutubeAnalyticsInput_1);
			de.jlo.talendcomp.youtubeanalytics.YoutubeAnalyticsInput
					.putIntoCache(
							"/Data/Talend/testdata/ga/config/client_secret_503880615382-n8ti68l59e04hpuvljrbe6hml9ov5jch.apps.googleusercontent.com.json"
									+ ""
									+ "tYoutubeAnalyticsInput_1"
									+ jobName, tYoutubeAnalyticsInput_1);
		} // if (tYoutubeAnalyticsInput_1 == null)
			// setup query
		tYoutubeAnalyticsInput_1
				.setChannelId("UCFvoTlSik404RjXaBPCei8A");
		tYoutubeAnalyticsInput_1.setStartDate("2018-01-01");
		tYoutubeAnalyticsInput_1.setMaxRows(100);
		// for selecting data for one day, set end date == start date
		tYoutubeAnalyticsInput_1.setEndDate("2019-04-04");
		tYoutubeAnalyticsInput_1.setDimensions("video");
		tYoutubeAnalyticsInput_1.setMetrics("views,likes");
		tYoutubeAnalyticsInput_1.setSorts("-views");
		// fire query and fetch first chunk of data
		try {
			// checks also the correctness of result columns
			tYoutubeAnalyticsInput_1.executeQuery();
		} catch (Exception e) {
			throw e;
		}
		// iterate through the data...
		int countLines_tYoutubeAnalyticsInput_1 = 0;
		while (true) {
			try {
				// hasNextDataset() executes a new query if needed
				if (tYoutubeAnalyticsInput_1.hasNextDataset() == false) {
					break;
				}
			} catch (Exception e) {
				globalMap.put("tYoutubeAnalyticsInput_1_ERROR_MESSAGE",
						e.getMessage());
				throw e;
			} // try catch
				// next row from results
			java.util.List<Object> dataset_tYoutubeAnalyticsInput_1 = tYoutubeAnalyticsInput_1
					.getNextDataset();
			// create a new row, thats avoid the need of setting
			// attributes to null
			row1 = new row1Struct();
			row1.video = (String) dataset_tYoutubeAnalyticsInput_1
					.get(0);
			row1.views = (BigDecimal) dataset_tYoutubeAnalyticsInput_1
					.get(1);
			row1.likes = ((BigDecimal) dataset_tYoutubeAnalyticsInput_1
					.get(2)).longValue();
			
			System.out.println(row1);
			
		}

	}

}
