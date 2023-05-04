package Solution;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * AirportSearcher, project for Renue internship
 *
 * @author Khabibullin Alisher
 */
public class AirportSearcher {
    private static final String FILE_NAME = "C:\\Users\\muzik\\Desktop\\java\\job\\Renue\\src\\main\\resources\\airports.csv";
    private static final String QUIT_COMMAND = "!quit";
    private static Map<String, Map<String, Long>> airports;
    private static HashSet<String[]> airportData;

    public static void main(String[] args) {
        loadAirports();

        Scanner scanner = new Scanner(System.in);
        while (true) {

            System.out.print("Enter filter: ");
            String filterInput = scanner.nextLine().trim();
            if (filterInput.equals(QUIT_COMMAND)) {
                break;
            }
            System.out.print("Enter airport name: ");
            String airportName = scanner.nextLine().trim().toLowerCase().replace(" ", "");
            if (airportName.equals("")){
                System.out.println("You need to input airport name. Try again.");
                continue;
            }
            Instant startTime = Instant.now();

            loadAirportData(returnLinesMatches(airportName));


            Map<String, HashSet<String>> filters;
            if (!filterInput.equals("")) {
                try {
                    filters = parseFilter(filterInput);
                } catch (IllegalArgumentException exc){
                    System.out.println("Wrong filters format.");
                    continue;
                }

            } else {
                filters = null;
            }
            List<String[]> sortedAirports = searchAirportData(airportName, filters);
            Instant endTime = Instant.now();

            if (!sortedAirports.isEmpty()) {
                for (String[] airport : sortedAirports) {
                    System.out.println(Arrays.toString(airport));
                }
            }
            System.out.printf("Found %d rows in %d ms\n", sortedAirports.size(), Duration.between(startTime, endTime).toMillis() / 2);
        }

    }

    public static Set<Long> returnLinesMatches(String airportName) {
        String key = String.valueOf(airportName.charAt(0)).toUpperCase();
        Map<String, Long> airportWithLetter = airports.get(key);
        return airportWithLetter != null ? new LinkedHashSet<>(airportWithLetter.values()) : Collections.emptySet();
    }


    private static HashSet<String[]> filterAirportData(HashSet<String[]> airportData, Map<String, HashSet<String>> filters) {

        HashSet<String[]> filteredData = new HashSet<>();
        for (String[] row : airportData) {
            boolean includeRow = true;

            // Iterate over each filter
            for (Map.Entry<String, HashSet<String>> filterEntry : filters.entrySet()) {

                String columnName = filterEntry.getKey();
                HashSet<String> filterValues = filterEntry.getValue();
                String cellValue = row[Integer.parseInt(columnName)];

                for (String filterValue : filterValues) {
                    if (filterValue.startsWith("<")) {
                        double cellDoubleValue = Double.parseDouble(cellValue);
                        double filterDoubleValue = Double.parseDouble(filterValue.substring(1));
                        if (cellDoubleValue >= filterDoubleValue) {
                            includeRow = false;
                            break;
                        }
                    } else if (filterValue.startsWith(">")) {
                        double cellDoubleValue = Double.parseDouble(cellValue);
                        double filterDoubleValue = Double.parseDouble(filterValue.substring(1));
                        if (cellDoubleValue <= filterDoubleValue) {
                            includeRow = false;
                            break;
                        }
                    } else if (filterValue.startsWith("=")) {

                        String filterStringValue = filterValue.substring(1).replace("’", "");
                        if (!cellValue.equals(filterStringValue)) {
                            includeRow = false;
                            break;
                        }
                    }
                }
                if (includeRow) {
                    filteredData.add(row);

                } else {
                    break;
                }
            }
        }

        return filteredData;
    }

    public static Map<String, HashSet<String>> parseFilter(String filterInput) throws IllegalArgumentException{
        Map<String, HashSet<String>> filterMap = new HashMap<>();
        if (filterInput == null || filterInput.trim().isEmpty()) {
            return filterMap;
        }
        String[] filters = filterInput.split("\\|\\|");
        for (String filter : filters) {
            String[] conditions = filter.split("&");
            for (String condition : conditions) {
                if (condition.isEmpty()) {
                    continue;
                }
                String[] parts = condition.split("=");
                if (parts.length != 2) {
                    parts = condition.split(">");
                    if (parts.length != 2) {
                        parts = condition.split("<");
                        if (parts.length != 2) {
                            throw new IllegalArgumentException("Invalid condition: " + condition);
                        }
                    } else {
                        int columnIndex = Integer.parseInt(parts[0].replaceAll("column\\[|]", "")) - 1;
                        filterMap.putIfAbsent(Integer.toString(columnIndex), new HashSet<>());
                        filterMap.get(Integer.toString(columnIndex)).add(">" + parts[1]);
                    }
                } else {
                    int columnIndex = Integer.parseInt(parts[0].replaceAll("column\\[|]", "")) - 1;
                    filterMap.putIfAbsent(Integer.toString(columnIndex), new HashSet<>());
                    filterMap.get(Integer.toString(columnIndex)).add("=" + parts[1]);
                }
            }
        }
        return filterMap;
    }

    public static List<String[]> searchAirportData(String airportName, Map<String, HashSet<String>> filters) {
        HashSet<String[]> filteredData;

        if (filters != null) {
            filteredData = filterAirportData(airportData, filters);
        } else {
            filteredData = airportData;
        }

        List<String[]> matchingData = new ArrayList<>();
        for (String[] airport : filteredData) {
            if (airport[1].toLowerCase().startsWith(airportName.toLowerCase())) {
                matchingData.add(airport);
            }
        }

        matchingData.sort((a1, a2) -> a1[1].compareToIgnoreCase(a2[1]));

        return matchingData;
    }

    public static void loadAirports() {
        airports = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] airport = line.replaceAll("\"", "").split(",");
                String key = String.valueOf(airport[1].charAt(0)).toUpperCase();
                airports.putIfAbsent(key, new HashMap<>());
                airports.get(key).put(airport[1], Long.parseLong(airport[0]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadAirportData(Set<Long> rowNumbers) {
        airportData = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] row = line.replaceAll("\"", "").split(",");
                if (rowNumbers.contains(Long.parseLong(row[0]))) {
                    airportData.add(row);
                }

            }
        } catch (IOException e) {
            System.err.println("Error reading airport data from file: " + e.getMessage());
        }
    }
}