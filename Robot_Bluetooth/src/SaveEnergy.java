import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;

public class SaveEnergy {

	/*
	 * This is the constructor for our high score file, which does nothing right now
	 */
	public SaveEnergy() {

	}

	/*
	 * This method will write the given name and score obtained to the file
	 */
	public static void writeFile(LinkedList<Double> allPower, LinkedList<Double> allMillis,
			LinkedList<Double> allEllapsed, LinkedList<Double> allEnergy) throws UnsupportedEncodingException {

		PrintWriter pw;

		DateTimeFormatter date = DateTimeFormatter.ofPattern("MM-dd-yyyy hh");
		DateTimeFormatter minute = DateTimeFormatter.ofPattern("mm");
		LocalDateTime dateNow = LocalDateTime.now();
		LocalDateTime minuteNow = LocalDateTime.now();
		String newFileName = "Logging/MostRecentRun-" + date.format(dateNow) + "-" + minute.format(minuteNow) + ".txt";
		System.out.println(newFileName);
		// File newFile = new File(newFileName);

		try {
			pw = new PrintWriter(newFileName, "UTF-8");
			for (int i = 0; i < allEllapsed.size(); i++) {
				pw.println("Elapsed Time: " + allEllapsed.get(i) + "ms. Total Time: " + allMillis.get(i) + "ms. Power: "
						+ allPower.get(i) + "mW. Energy: " + allEnergy.get(i) + "mJ.");
				pw.println("....................................................................................");
			}
			pw.println("*****END OF RUN*****");
		} catch (FileNotFoundException e) {
			System.out.println("Error Opening File...");
			e.printStackTrace();
			return;
		}
		pw.close();
	}

	/*
	 * This method will read from the file, but right now we don't access it at all
	 */
	public void readFile(String name) {

		// We will just read the file from File Explorer, so we won't use this.
		// Constructor gets mad without it
	}
}
