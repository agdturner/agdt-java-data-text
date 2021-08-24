/*
 * Part of a library developed for text data processing tasks.
 * Copyright 2017 Andy Turner, University of Leeds.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.leeds.ccg.data.text.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.leeds.ccg.data.text.core.Text_Environment;
import uk.ac.leeds.ccg.data.text.core.Text_Object;
import uk.ac.leeds.ccg.data.text.io.Text_Files;
import uk.ac.leeds.ccg.generic.io.Generic_IO;

/**
 * This class has a main method and so can be run. It was developed originally
 * for processing a set of files downloaded from NexisLexis for an undergraduate
 * student dissertation project. The NexisLexis files are in HTML format and
 * comprise the text of sets of articles downloaded from
 * http://www.nexislexis.com from the Express and Guardian newspapers from the
 * 1st of April 2015 to the 10th of February 2016. The search criteria for the
 * articles included the words "refugee" or "brexit" or both.
 */
public class Text_Processor extends Text_Object {

    /**
     * files is used to help manage input and output to the file system.
     */
    Text_Files files;

    public Text_Processor(Text_Environment e) {
        super (e);
    }

    public static void main(String[] args) {
        try {
            new Text_Processor(new Text_Environment()).run();
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

    /**
     * This is the main processing method.
     * @throws java.io.IOException
     */
    public void run() throws IOException {

        boolean writeHeadlines;
        //writeHeadlines = true;
        writeHeadlines = false;

        /**
         * Initialise words
         */
        ArrayList words = new ArrayList();
        words.add("refugee");
        words.add("syria");
        words.add("brexit");
        words.add("migrant crisis");
        words.add("steven woolfe");

        /**
         * Initialise directories
         */
        files = new Text_Files(env.env.files.getDir());
        String dirname;
        //dirname = "LexisNexis-20171127T155442Z-001";
        dirname = "LexisNexis-20171122T195223Z-001";
        File inputDir;
        File outDir;
        inputDir = new File(files.getInDir().toFile(), dirname);
        inputDir = new File(inputDir, "LexisNexis");
        System.out.println(inputDir);
        outDir = new File(files.getOutDir().toFile(), dirname);
        outDir = new File(outDir, "LexisNexis");
        if (outDir.exists()) {
            outDir.mkdirs();
        }

        /**
         * Declare variables
         */
        ArrayList<DayOfWeek> mondayToSaturday;
        mondayToSaturday = getMondayToSaturday();
        int[] grandTotalWordCounts;
        HashMap<String, TreeMap<DayOfWeek, Integer>> grandTotalWordCountOnDays;
        int[] grandTotalArticleCountsForWords;
        HashMap<String, TreeMap<DayOfWeek, Integer>> grandTotalArticleCountsForWordsOnDays;
        int i;
        String word;
        Iterator<String> ite;
        /**
         * Process the data going through each input file. Currently the output
         * is simply printed to std.out.
         */
        String name;
        File outFile;
        PrintWriter pwCounts;
        PrintWriter pwHeadlines = null;
        File[] inputs0;
        File[] inputs1;
        inputs0 = inputDir.listFiles();
        /**
         * Iterate through all the directories in inputDir. It is known that
         * inputDir contains only directories and no files.
         */
        for (File input0 : inputs0) {
            name = input0.getName();
            outFile = new File(outDir, name + "Counts.csv");
            pwCounts = Generic_IO.getPrintWriter(outFile.toPath(), false);
            if (writeHeadlines) {
                outFile = new File(outDir,
                        name + "HeadlinesForArticlesContaining_Syria.txt");
                pwHeadlines = Generic_IO.getPrintWriter(outFile.toPath(), false);
            }
            /**
             * Print out the name of the directory/File.
             */
            //System.out.println(input0);
            System.out.println("---------------------------");
            System.out.println(name);
            //pw.println(name);
            System.out.println("---------------------------");
            /**
             * Iterate through all the files in the directory.
             */
            inputs1 = input0.listFiles();

            /**
             * Initialise results.
             */
            grandTotalWordCounts = new int[words.size()];
            grandTotalArticleCountsForWords = new int[words.size()];
            grandTotalWordCountOnDays = new HashMap<>();
            grandTotalArticleCountsForWordsOnDays = new HashMap<>();
            i = 0;
            ite = words.iterator();
            while (ite.hasNext()) {
                word = ite.next();
                grandTotalWordCounts[i] = 0;
                grandTotalArticleCountsForWords[i] = 0;
                i++;
                grandTotalWordCountOnDays.put(word, new TreeMap<>());
                grandTotalArticleCountsForWordsOnDays.put(word, new TreeMap<>());
            }
            /**
             * Iterate through all the subdirectories in inputDir. It is known
             * that each subdirectory contains a set of HTML files and
             * associated directories. For the purposes of this processing, only
             * the HTML files are processed.
             */
            int[] totalWordCounts;
            int[] totalWordCountsInArticles;
            Object[] results;
            for (File input1 : inputs1) {
                //System.out.println(input1);
                /**
                 * Filter to only process the HTML files.
                 */
                if (input1.getName().endsWith("htm")) {
                    //System.out.println(input1);
                    /**
                     * Parse the HTML file and obtain part of the result.
                     */
                    results = parseHTML(words, input1);
                    /**
                     * Combine the results from parsing this file to the overall
                     * results.
                     */
                    // Process counts.
                    totalWordCounts = (int[]) results[0];
                    totalWordCountsInArticles = (int[]) results[1];
                    i = 0;
                    ite = words.iterator();
                    while (ite.hasNext()) {
                        word = ite.next();
                        grandTotalWordCounts[i] += totalWordCounts[i];
                        grandTotalArticleCountsForWords[i] += totalWordCountsInArticles[i];
                        i++;
                        addToCount(
                                ((TreeMap<String, TreeMap<DayOfWeek, Integer>>) results[2]).get(word),
                                grandTotalWordCountOnDays.get(word));
                        addToCount(
                                ((TreeMap<String, TreeMap<DayOfWeek, Integer>>) results[3]).get(word),
                                grandTotalArticleCountsForWordsOnDays.get(word));

                    }
                    if (writeHeadlines) {
                        // Process dates and headlines writing out a list.
                        TreeSet<DateHeadline> syriaDateHeadlines;
                        syriaDateHeadlines = (TreeSet<DateHeadline>) results[4];
                        Iterator<DateHeadline> ite2;
                        DateHeadline dh;
                        ite2 = syriaDateHeadlines.iterator();
                        while (ite2.hasNext()) {
                            dh = ite2.next();
                            System.out.println(dh.LD + " " + dh.Headline);
                        }
                    }
                }
            }
            /**
             * Write out summaries of counts.
             */
            /**
             * Write header
             */
            System.out.print("term, total word count, total article count, ");
            pwCounts.print("term, total word count, total article count, ");
//                System.out.println(word + " word count on " + day + " " + i);
//                pw.println(word + " word count on " + day + " " + i);
            TreeMap<DayOfWeek, Integer> grandTotalWordCountOnDay;
            TreeMap<DayOfWeek, Integer> grandTotalArticleCountsForWordsOnDay;
            Iterator<DayOfWeek> ite2;
            DayOfWeek day;
            ite2 = mondayToSaturday.iterator();
            while (ite2.hasNext()) {
                day = ite2.next();
                System.out.print(", word count on " + day);
                pwCounts.print(", word count on " + day);
            }
            ite2 = mondayToSaturday.iterator();
            while (ite2.hasNext()) {
                day = ite2.next();
                System.out.print(", article count on " + day);
                pwCounts.print(", article count on " + day);
            }
            System.out.println();
            pwCounts.println();
            /**
             * Write lines
             */
            i = 0;
            ite = words.iterator();
            while (ite.hasNext()) {
                word = ite.next();
                grandTotalWordCountOnDay = grandTotalWordCountOnDays.get(word);
                grandTotalArticleCountsForWordsOnDay = grandTotalArticleCountsForWordsOnDays.get(word);
//                System.out.println(word + " word count " + grandTotalWordCounts[i]);
//                pwCounts.println(word + " word count " + grandTotalWordCounts[i]);
//                System.out.println(word + " article count " + grandTotalArticleCountsForWords[i]);
//                pwCounts.println(word + " article count " + grandTotalArticleCountsForWords[i]);
                System.out.print(word);
                pwCounts.print(word);
                System.out.print(", " + grandTotalWordCounts[i]);
                pwCounts.print(", " + grandTotalWordCounts[i]);
                System.out.print(", " + grandTotalArticleCountsForWords[i]);
                pwCounts.print(", " + grandTotalArticleCountsForWords[i]);
                i++;
                printWordCountOnDay(pwCounts, mondayToSaturday, word, grandTotalWordCountOnDays.get(word));
                printWordCountOnDay(pwCounts, mondayToSaturday, word, grandTotalArticleCountsForWordsOnDays.get(word));
                System.out.println();
                pwCounts.println();
            }
            System.out.println("---------------------------");
            pwCounts.close();
            if (writeHeadlines) {
                pwHeadlines.close();
            }
        }
    }

    void printWordCountOnDay(
            PrintWriter pw,
            ArrayList<DayOfWeek> mondayToSaturday,
            String word,
            TreeMap<DayOfWeek, Integer> grandTotalWordCountOnDay) {
        Iterator<DayOfWeek> ite;
        DayOfWeek day;
        Integer i;
        ite = mondayToSaturday.iterator();
        while (ite.hasNext()) {
            day = ite.next();
            i = grandTotalWordCountOnDay.get(day);
            if (i == null) {
                System.out.print(", 0");
                pw.print(", 0");
            } else {
                System.out.print(", " + i);
                pw.print(", " + i);
            }
        }
    }

    ArrayList<DayOfWeek> getMondayToSaturday() {
        ArrayList<DayOfWeek> result;
        result = new ArrayList<>();
        result.add(DayOfWeek.MONDAY);
        result.add(DayOfWeek.TUESDAY);
        result.add(DayOfWeek.WEDNESDAY);
        result.add(DayOfWeek.THURSDAY);
        result.add(DayOfWeek.FRIDAY);
        result.add(DayOfWeek.SATURDAY);
        return result;
    }

    /**
     * This method parses the HTML file and returns results that are packed into
     * an Object[] result of size 5: result[0] is an int[] of size 3 containing
     * counts of the numbers of mentions of the terms "refugee", "syria" and
     * "brexit" respectively. result[1] is TreeMap with keys that are the
     * DayOfWeek and values that are counts of the number of times the word
     * "refugee" appears on those days. result[2] is TreeMap with keys that are
     * the DayOfWeek and values that are counts of the number of times the word
     * "syria" appears on those days. result[3] is TreeMap with keys that are
     * the DayOfWeek and values that are counts of the number of times the word
     * "brexit" appears on those days. result[4] is a TreeSet of DateHeadlines
     * which provides the dated and headlines of articles that have mention of
     * "syria" in them.
     *
     * @param words
     * @param input The input file to be parsed.
     * @return
     */
    public Object[] parseHTML(ArrayList<String> words, File input) throws FileNotFoundException, IOException {
        Object[] result = new Object[5];
        TreeSet<DateHeadline> syriaDateHeadlines;
        syriaDateHeadlines = new TreeSet<>();
        TreeMap<String, TreeMap<DayOfWeek, Integer>> totalWordCountByDay;
        TreeMap<String, TreeMap<DayOfWeek, Integer>> totalArticleCountForWordsByDay;
        try (BufferedReader br = Generic_IO.getBufferedReader(input.toPath())) {
            String line = null;
            boolean read = false;
            int n;
            n = words.size();
            int[] totalWordCounts = new int[n];
            result[0] = totalWordCounts;
            int[] totalArticleCountsForWords = new int[n];
            result[1] = totalArticleCountsForWords;
            int[] wordCounts = new int[n];
            int[] articleCountsForWords = new int[n];
            totalWordCountByDay = new TreeMap<>();
            totalArticleCountForWordsByDay = new TreeMap<>();
            int i;
            String word;
            Iterator<String> ite;
            i = 0;
            ite = words.iterator();
            while (ite.hasNext()) {
                word = ite.next();
                totalWordCounts[i] = 0;
                totalArticleCountsForWords[i] = 0;
                wordCounts[i] = 0;
                articleCountsForWords[i] = 0;
                i++;
                totalWordCountByDay.put(word, new TreeMap<>());
                totalArticleCountForWordsByDay.put(word, new TreeMap<>());
            }   LocalDate date0 = null;
            boolean gotFirstDate = false;
            boolean gotTitle = false;
            String title = null;
            while (!read) {
                try {
                    line = br.readLine();
                } catch (IOException ex) {
                    Logger.getLogger(Text_Processor.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (line == null) {
                    /**
                     * The file is completely read, so set read to true to exit the
                     * loop.
                     */
                    read = true;
                } else {
                    //System.out.println(line);
                    LocalDate date;
                    DayOfWeek day;
                    if (!gotFirstDate) {
                        // Get to the first article and store the date in date0.
                        date0 = parseDate(line);
                        if (date0 != null) {
                            gotFirstDate = true;
                        }
                    } else {
                        /**
                         * If the line contains a new date, then assume this is the
                         * start of a new article: compile the data from the last
                         * article; and, reset the variables to store information
                         * about the next article.
                         */
                        date = parseDate(line);
                        if (date == null) {
                            /**
                             * Parse the next article by first getting the title.
                             */
                            if (!gotTitle) {
                                title = parseTitle(line);
                                if (!title.isEmpty()) {
                                    gotTitle = true;
                                    //System.out.println(title);
                                }
                            }
                            i = 0;
                            ite = words.iterator();
                            while (ite.hasNext()) {
                                word = ite.next();
                                // Add to word counts
                                wordCounts[i] += getWordCount(word, line);
                                i++;
                            }
                        } else {
                            gotTitle = false;
                            day = date0.getDayOfWeek();
                            i = 0;
                            ite = words.iterator();
                            while (ite.hasNext()) {
                                word = ite.next();
                                // Add to word counts
                                totalWordCounts[i] += wordCounts[i];
                                if (wordCounts[i] > 0) {
                                    totalArticleCountsForWords[i]++;
                                    addToCount(totalArticleCountForWordsByDay.get(word), day, 1);
                                }
                                addToCount(totalWordCountByDay.get(word), day, wordCounts[i]);
                                i++;
                            }
                            /**
                             * Store DateHeadline's for those articles on Saturdays
                             * that contain the word "syria".
                             */
                            if (wordCounts[words.indexOf("syria")] > 0) {
                                if (date.getDayOfWeek().equals(DayOfWeek.SATURDAY)) {
                                    syriaDateHeadlines.add(new DateHeadline(date, title));
                                }
                            }
                            for (i = 0; i < n; i++) {
                                wordCounts[i] = 0;
                            }
                            date0 = date;
                        }
                    }
                }
            }
        }
        result[2] = totalWordCountByDay;
        result[3] = totalArticleCountForWordsByDay;
        result[4] = syriaDateHeadlines;
        return result;
    }

    public void addToCount(
            TreeMap<DayOfWeek, Integer> totalWordCountOnDay,
            TreeMap<DayOfWeek, Integer> grandTotalWordCountOnDays) {
        DayOfWeek day;
        int i;
        Iterator<DayOfWeek> ite = totalWordCountOnDay.keySet().iterator();
        while (ite.hasNext()) {
            day = ite.next();
            if (grandTotalWordCountOnDays.containsKey(day)) {
                i = grandTotalWordCountOnDays.get(day);
                i += totalWordCountOnDay.get(day);
            } else {
                i = totalWordCountOnDay.get(day);
            }
            grandTotalWordCountOnDays.put(day, i);
        }
    }

    public void addToCount(
            TreeMap<DayOfWeek, Integer> wordCount,
            DayOfWeek day, int count) {
        int i;
        if (wordCount.containsKey(day)) {
            i = wordCount.get(day);
            i += count;
        } else {
            i = count;
        }
        wordCount.put(day, i);
    }

    /**
     * A generalised method that counts the number of times word appears in
     * line. This is done for the case that word is provided in, for word in all
     * upper case and for word with a capitalised/uppercase first letter.
     *
     * @param word
     * @param line
     * @return
     */
    int getWordCount(String word, String line) {
        String lowerCaseLine = line.toLowerCase();
        int result = 0;
        result += lowerCaseLine.split(word).length - 1;
        return result;
    }

    /**
     * For parsing a line. If it is thought to be a title as it contains some
     * key text that it is assumed all titles have, then the title is returned
     * otherwise an empty String is returned.
     *
     * @param line The line that is parsed.
     * @return
     */
    String parseTitle(String line) {
        String result = "";
        if (line.startsWith("<br><div class=\"c5\"><p class=\"c6\"><span class=\"c7\">")) {
            String s;
            s = line.replace("<br><div class=\"c5\"><p class=\"c6\"><span class=\"c7\">", "");
            s = s.replace("</span><span class=\"c10\">refugees</span><span class=\"c7\"></span><span class=\"c10\">refugees</span><span class=\"c7\">", "refugees");
            s = s.replace("</span><span class=\"c10\">Refugees</span><span class=\"c7\"></span><span class=\"c10\">Refugees</span><span class=\"c7\">", "refugees");
            s = s.replace("</span><span class=\"c10\">Brexit</span><span class=\"c7\">", "Brexit");
            s = s.replace("&nbsp;", " ");
            s = s.replace("&amp;", " ");
            String[] split = s.split("</span>");
            result = split[0];
            if (result.contains("<br>")) {
                //result = result.split("<br>")[0].trim();
                result = result.replace("<br>", " ");
            }
        }
        return result;
    }

    /**
     * For parsing a line. If it is thought to be a date as it contains some key
     * text that it is assumed all dates have, then the date is returned
     * otherwise an empty String is returned.
     *
     * @param line
     * @return
     */
    LocalDate parseDate(String line) {
        LocalDate result = null;
        if (line.startsWith("<br><div class=\"c3\"><p class=\"c1\"><span class=\"c4\">")) {
            String month;
            String dayOfMonth;
            String year;
            String dayOfWeek;
            String s;
            s = line.replaceAll("<br><div class=\"c3\"><p class=\"c1\"><span class=\"c4\">", "");
            String[] split;
            split = s.split("</span><span class=\"c2\"> ");
            month = split[0];
            String[] split2;
            split2 = split[1].replace("</span><span", "").trim().split(",");
            dayOfMonth = split2[0];
            String[] split3;
            split3 = split2[1].trim().split(" ");
            year = split3[0];
            dayOfWeek = split3[1];
//            String stringDate = "";
//            stringDate += "year " + year;
//            stringDate += " month " + month;
//            stringDate += " dayOfMonth " + dayOfMonth;
//            stringDate += " dayOfWeek " + dayOfWeek;
//            System.out.println(stringDate);
            Month m = Month.valueOf(month.toUpperCase());
            result = LocalDate.of(Integer.valueOf(year), m, Integer.valueOf(dayOfMonth));
            //System.out.println(LD.toString());
        }
        return result;
    }

    /**
     * A simple inner class for wrapping a LocalDate and a String such that
     * different instances can be ordered which the are first by date and then
     * by the String.
     */
    public class DateHeadline implements Comparable<DateHeadline> {

        LocalDate LD;
        String Headline;

        public DateHeadline() {
        }

        public DateHeadline(LocalDate ld, String headline) {
            LD = ld;
            Headline = headline;
        }

        @Override
        public int compareTo(DateHeadline t) {
            int dateComparison = LD.compareTo(t.LD);
            if (dateComparison == 0) {
                if (Headline == null) {
                    if (t.Headline == null) {
                        return 0;
                    } else {
                        return 1;
                    }
                } else {
                    if (t.Headline == null) {
                        return -1;
                    } else {
                        return Headline.compareTo(t.Headline);
                    }
                }
            } else {
                return dateComparison;
            }
        }
    }
}
