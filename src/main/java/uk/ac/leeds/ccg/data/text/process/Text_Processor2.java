/*
 * Copyright 2021 Andy Turner, University of Leeds.
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import uk.ac.leeds.ccg.generic.time.Generic_LocalDateRange;
import uk.ac.leeds.ccg.data.text.core.Text_Environment;
import uk.ac.leeds.ccg.data.text.core.Text_Object;
import uk.ac.leeds.ccg.data.text.io.Text_Files;
import uk.ac.leeds.ccg.generic.io.Generic_Defaults;
import uk.ac.leeds.ccg.generic.io.Generic_IO;
import uk.ac.leeds.ccg.generic.lang.Generic_String;

/**
 * This class was originally developed for processing a set of files downloaded
 * from NexisLexis for two undergraduate geography student dissertation projects
 * mainly undertaken in the 2017 to 2018 academic year at the University of
 * Leeds. The NexisLexis input files processed were in HTML format and comprise
 * the text of two sets of articles downloaded from http://www.nexislexis.com.
 * One set of articles came from The Daily Express and The Guardian; and the
 * other set included articles from The Daily Mail, The Mail on Sunday, The
 * Daily Mirror, The Telegraph and The Guardian newspapers. The first set
 * included articles with the terms ("refugee") or ("refugee" and "brexit"). The
 * second set included articles with the terms ("refugee" or "asylum seeker").
 * The program processes the data and has two main capabilities. 1) For a given
 * search term, the titles and dates of the articles containing that search term
 * can be written to a file. 2) For a list of terms, the program will return the
 * counts of the number of these terms and the number of articles containing
 * these terms in each set of newspapers. For each set of outputs a time period
 * can be set to return results for a specific time period.
 */
public class Text_Processor2 extends Text_Object {

    /**
     * files is used to help manage input and output to the file system.
     */
    Text_Files files;

    public Text_Processor2(Text_Environment e) {
        super(e);
    }

    public static void main(String[] args) {
        try {
            new Text_Processor2(new Text_Environment()).run();
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

    /**
     * For storing a Guardian API key. (This is currently not used.)
     */
    String GuardianAPIKey;

    /**
     * For storing names of newspapers to be processed
     */
    ArrayList<String> papers;

    /**
     * For counting the number of articles in a paper.
     */
    HashMap<String, Integer> paperArticleCounts;

    /**
     * For counting the number of articles in a paper by the day of week.
     */
    HashMap<String, TreeMap<DayOfWeek, Integer>> paperArticleCountsByDayOfWeek;

    String sTheExpress = "The Express";
    String sTheGuardian = "The Guardian";
    String sDailyMail = "DAILY MAIL (London)";
    String sMailOnSunday = "MAIL ON SUNDAY (London)";
    String sDailyMirror = "Daily Mirror";
    String sTheDailyTelegraph = "The Daily Telegraph (London)";
    String sBEN = "Birmingham Evening Mail";
    String sMEN = "Manchester Evening News";
    String sTheEveningStandard = "The Evening Standard (London)";

    /**
     * If true then a file with headlines for articles containing the term
     * headlineTerm are written out to file.
     */
    boolean writeHeadlines;

    /**
     * For storing a term for which headlines of articles containing the term
     * are written out.
     */
    String headlineTerm;

    /**
     * If runID == 0, then this is a run for Felicity; if runID == 1, then this
     * is a run for Emma; ; if runID == 1, then this is a run for Harriet.
     */
    int runID;

    /**
     * This is the main processing method.
     * @throws java.io.IOException
     */
    public void run() throws IOException {

        /**
         * Set writeHeadlines to be true to write out the titles of articles for
         * those articles containing the term headlineTerm.
         */
        //writeHeadlines = true;
        writeHeadlines = false;
        headlineTerm = "Syria"; // Set this to another term if wanted.

        /**
         * Set which run to do.
         */
        runID = 0; // Felicity
//        runID = 1; // Emma
//        runID = 2; // Harriet

        /**
         * Set start and end dates
         */
        ArrayList<Generic_LocalDateRange> dates;
        dates = new ArrayList<>();
        if (runID == 0) {
            dates.add(new Generic_LocalDateRange(
                    LocalDate.of(2013, Month.JANUARY, 1),
                    LocalDate.of(2017, Month.DECEMBER, 31)));
            dates.add(new Generic_LocalDateRange(
                    LocalDate.of(2015, Month.JUNE, 1),
                    LocalDate.of(2015, Month.AUGUST, 31)));
            dates.add(new Generic_LocalDateRange(
                    LocalDate.of(2015, Month.SEPTEMBER, 1),
                    LocalDate.of(2015, Month.NOVEMBER, 30)));
            dates.add(new Generic_LocalDateRange(
                    LocalDate.of(2016, Month.APRIL, 1),
                    LocalDate.of(2016, Month.JUNE, 30)));
            dates.add(new Generic_LocalDateRange(
                    LocalDate.of(2016, Month.MARCH, 23),
                    LocalDate.of(2016, Month.JUNE, 23)));
        } else {
            dates.add(new Generic_LocalDateRange(
                    LocalDate.of(2015, Month.JUNE, 1),
                    LocalDate.of(2018, Month.AUGUST, 31)));
        }

        /**
         * Initialise papers, paperArticleCounts and
         * paperArticleCountsByDayOfWeek.
         */
        papers = new ArrayList<>();
        switch (runID) {
            case 0:
                papers.add(sTheExpress);
                papers.add(sTheGuardian);
                break;
            case 1:
                papers.add(sTheGuardian);
                papers.add(sDailyMirror);
                papers.add(sMailOnSunday);
                papers.add(sDailyMirror);
                papers.add(sTheDailyTelegraph);
                break;
            default:
                papers.add(sBEN);
                papers.add(sMEN);
                papers.add(sTheEveningStandard);
                break;
        }
        paperArticleCounts = new HashMap<>();
        paperArticleCountsByDayOfWeek = new HashMap<>();

        /**
         * Get terms and declare key variables.
         */
        Object[] allTerms;
        switch (runID) {
            case 0:
                allTerms = getAllTermsFelicity();
                break;
            case 1:
                allTerms = getAllTermsEmma();
                break;
            default:
                allTerms = getAllTermsHarriet();
                break;
        }
        TreeMap<Integer, ArrayList<String>> allterms;
        allterms = (TreeMap<Integer, ArrayList<String>>) allTerms[0];
        HashMap<Integer, String> termTypes;
        termTypes = (HashMap<Integer, String>) allTerms[1];
        int numberOfTerms = (Integer) allTerms[2];

        /**
         * Initialise directories
         */
        files = new Text_Files(env.env.files.getDir());
        String dirname;
        switch (runID) {
            case 0:
                dirname = "LexisNexis-20171127T155442Z-001";
//                dirname ="LexisNexis-20171122T195223Z-001";
                break;
            case 1:
                dirname = "Emma";
                break;
            default:
                dirname = "Harriet";
                break;
        }
        Path inputDir = Paths.get(files.getInDir().toString(), dirname, "LexisNexis");
        System.out.println(inputDir);

        // Get GuardianAPIKey
        GuardianAPIKey = getGuardianAPIKey();

        /**
         * Declare variables
         */
        ArrayList<DayOfWeek> mondayToSaturday;
        mondayToSaturday = getMondayToSaturday();
        int[] grandTotalTermCounts;
        HashMap<String, TreeMap<DayOfWeek, Integer>> grandTotalTermCountOnDays;
        int[] grandTotalArticleCountsForTerms;
        HashMap<String, TreeMap<DayOfWeek, Integer>> grandTotalArticleCountsForTermsOnDays;
        String term;
        String name;
        File outFile;
        PrintWriter pwCounts;
        PrintWriter pwHeadlines = null;
        File[] inputs0;
        File[] inputs1;
        inputs0 = inputDir.toFile().listFiles();
        Iterator<String> papersIte;
        String p;

        /**
         * Process the data for each start and end time period going through
         * each input file.
         */
        Iterator<Generic_LocalDateRange> iteDR;
        iteDR = dates.iterator();
        while (iteDR.hasNext()) {
            Generic_LocalDateRange dateRange;
            dateRange = iteDR.next();
            LocalDate start;
            LocalDate end;
            start = dateRange.getStart();
            end = dateRange.getEnd();
            File outDir = new File(files.getOutDir().toFile(),
                    dirname + "/LexisNexis" + start.toString()
                    + "_" + end.toString());
            if (!outDir.exists()) {
                outDir.mkdirs();
            }

            /**
             * Iterate through all the directories in inputDir. It is known that
             * inputDir contains only directories and no files.
             */
            for (File input0 : inputs0) {
                // Reset paperArticleCounts
                papersIte = papers.iterator();
                while (papersIte.hasNext()) {
                    p = papersIte.next();
                    paperArticleCounts.put(p, 0);
                    TreeMap<DayOfWeek, Integer> articleCountsByDayOfWeek;
                    articleCountsByDayOfWeek = new TreeMap<>();
                    articleCountsByDayOfWeek.put(DayOfWeek.MONDAY, 0);
                    articleCountsByDayOfWeek.put(DayOfWeek.TUESDAY, 0);
                    articleCountsByDayOfWeek.put(DayOfWeek.WEDNESDAY, 0);
                    articleCountsByDayOfWeek.put(DayOfWeek.THURSDAY, 0);
                    articleCountsByDayOfWeek.put(DayOfWeek.FRIDAY, 0);
                    articleCountsByDayOfWeek.put(DayOfWeek.SATURDAY, 0);
                    articleCountsByDayOfWeek.put(DayOfWeek.SUNDAY, 0);
                    paperArticleCountsByDayOfWeek.put(p, articleCountsByDayOfWeek);
                }

                name = input0.getName();
                outFile = new File(outDir, name + "Counts.csv");
                pwCounts = Generic_IO.getPrintWriter(outFile.toPath(), false);
                if (writeHeadlines) {
                    outFile = new File(outDir,
                            name + "HeadlinesForArticlesContaining_" + headlineTerm + ".csv");
                    pwHeadlines = Generic_IO.getPrintWriter(outFile.toPath(), false);
                    pwHeadlines.println("Date, Section, Length, Title");
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
                grandTotalTermCounts = new int[numberOfTerms];
                grandTotalArticleCountsForTerms = new int[numberOfTerms];
                grandTotalTermCountOnDays = new HashMap<>();
                grandTotalArticleCountsForTermsOnDays = new HashMap<>();
                int i = 0;
                Iterator<Integer> ite;
                ArrayList<String> terms;
                Iterator<String> ite2;
                ite = allterms.keySet().iterator();
                while (ite.hasNext()) {
                    terms = allterms.get(ite.next());
                    ite2 = terms.iterator();
                    while (ite2.hasNext()) {
                        term = ite2.next();
                        grandTotalTermCounts[i] = 0;
                        grandTotalArticleCountsForTerms[i] = 0;
                        i++;
                        grandTotalTermCountOnDays.put(term, new TreeMap<>());
                        grandTotalArticleCountsForTermsOnDays.put(term, new TreeMap<>());
                    }
                }
                /**
                 * Iterate through all the subdirectories in inputDir. It is
                 * known that each subdirectory contains a set of HTML files and
                 * associated directories. For the purposes of this processing,
                 * only the HTML files are processed.
                 */
                int[] totalTermCounts;
                int[] totalTermCountsInArticles;
                Object[] results;
                for (File input1 : inputs1) {
                    //System.out.println(input1);
                    /**
                     * Filter to only process the HTML files.
                     */
                    if (input1.getName().endsWith("htm")
                            || input1.getName().endsWith("HTML")) {
                        /**
                         * Parse the HTML file and obtain part of the result.
                         */
                        results = parseHTML(numberOfTerms, start, end, allterms,
                                input1);
                        /**
                         * Combine the results from parsing this file to the
                         * overall results.
                         */
                        // Process counts.
                        totalTermCounts = (int[]) results[0];
                        totalTermCountsInArticles = (int[]) results[1];
                        i = 0;
                        ite = allterms.keySet().iterator();
                        while (ite.hasNext()) {
                            terms = allterms.get(ite.next());
                            ite2 = terms.iterator();
                            while (ite2.hasNext()) {
                                term = ite2.next();
                                grandTotalTermCounts[i] += totalTermCounts[i];
                                grandTotalArticleCountsForTerms[i] += totalTermCountsInArticles[i];
                                i++;
                                addToCount(
                                        ((TreeMap<String, TreeMap<DayOfWeek, Integer>>) results[2]).get(term),
                                        grandTotalTermCountOnDays.get(term));
                                addToCount(
                                        ((TreeMap<String, TreeMap<DayOfWeek, Integer>>) results[3]).get(term),
                                        grandTotalArticleCountsForTermsOnDays.get(term));
                            }
                        }
                        if (writeHeadlines) {
                            // Process dates and headlines writing out a list.
                            TreeSet<DateOutlineDetails> headlineTermDateHeadlines;
                            headlineTermDateHeadlines = (TreeSet<DateOutlineDetails>) results[4];
                            Iterator<DateOutlineDetails> ite3;
                            DateOutlineDetails dh;
                            ite3 = headlineTermDateHeadlines.iterator();
                            while (ite3.hasNext()) {
                                dh = ite3.next();
                                String s;
                                s = dh.LD + ",\"" + dh.Section + "\",\"" + dh.Length + "\",\"" + dh.Headline + "\"";
                                System.out.println(s);
                                pwHeadlines.println(s);
                            }
                        }
                        //}
                    }
                }
                /**
                 * Write out summaries of counts.
                 */
                /**
                 * Write header
                 */
                String header;
                header = "Term Type,Term,Total Term Count,Total Article Count";
                TreeMap<DayOfWeek, Integer> grandTotalTermCountOnDay;
                TreeMap<DayOfWeek, Integer> grandTotalArticleCountsForTermsOnDay;
                Iterator<DayOfWeek> ite3;
                DayOfWeek day;
                ite3 = mondayToSaturday.iterator();
                while (ite3.hasNext()) {
                    day = ite3.next();
                    header += ",Term Count On " + day;
                }
                ite3 = mondayToSaturday.iterator();
                while (ite3.hasNext()) {
                    day = ite3.next();
                    header += ",Article Count On " + day;
                }
                System.out.println(header);
                pwCounts.println(header);
                /**
                 * Write lines
                 */
                i = 0;
                ite = allterms.keySet().iterator();
                while (ite.hasNext()) {
                    int typeInt = ite.next();
                    String termType = termTypes.get(typeInt);
                    terms = allterms.get(typeInt);
                    ite2 = terms.iterator();
                    while (ite2.hasNext()) {
                        term = ite2.next();
                        grandTotalTermCountOnDay = grandTotalTermCountOnDays.get(term);
                        grandTotalArticleCountsForTermsOnDay = grandTotalArticleCountsForTermsOnDays.get(term);
//                System.out.println(term + " term count " + grandTotalTermCounts[i]);
//                pwCounts.println(term + " term count " + grandTotalTermCounts[i]);
//                System.out.println(term + " Article count " + grandTotalArticleCountsForTerms[i]);
//                pwCounts.println(term + " Article count " + grandTotalArticleCountsForTerms[i]);
                        System.out.print(termType);
                        pwCounts.print(termType);
                        System.out.print("," + term);
                        pwCounts.print("," + term);
                        System.out.print("," + grandTotalTermCounts[i]);
                        pwCounts.print("," + grandTotalTermCounts[i]);
                        System.out.print("," + grandTotalArticleCountsForTerms[i]);
                        pwCounts.print("," + grandTotalArticleCountsForTerms[i]);
                        i++;
                        printTermCountOnDay(pwCounts, mondayToSaturday, term,
                                grandTotalTermCountOnDays.get(term));
                        printTermCountOnDay(pwCounts, mondayToSaturday, term,
                                grandTotalArticleCountsForTermsOnDays.get(term));
                        System.out.println();
                        pwCounts.println();
                    }
                }
                System.out.println("---------------------------");
                pwCounts.close();
                if (writeHeadlines) {
                    pwHeadlines.close();
                }
                papersIte = papers.iterator();
                while (papersIte.hasNext()) {
                    p = papersIte.next();
                    int c = paperArticleCounts.get(p);
                    System.out.println(p + " ArticleCount " + c);
                    if (c > 0) {
                        TreeMap<DayOfWeek, Integer> DoWArticleCounts;
                        DoWArticleCounts = paperArticleCountsByDayOfWeek.get(p);
                        Iterator<DayOfWeek> DoWIte;
                        DoWIte = DoWArticleCounts.keySet().iterator();
                        DayOfWeek DoW;
                        while (DoWIte.hasNext()) {
                            DoW = DoWIte.next();
                            System.out.println(p + " ArticleCount on "
                                    + DoW.toString() + " "
                                    + DoWArticleCounts.get(DoW));
                        }
                    }
                }
            }
        }
    }

    void printTermCountOnDay(PrintWriter pw,
            ArrayList<DayOfWeek> mondayToSaturday, String term,
            TreeMap<DayOfWeek, Integer> grandTotalTermCountOnDay) {
        Iterator<DayOfWeek> ite;
        DayOfWeek day;
        Integer i;
        ite = mondayToSaturday.iterator();
        while (ite.hasNext()) {
            day = ite.next();
            i = grandTotalTermCountOnDay.get(day);
            if (i == null) {
                System.out.print(",0");
                pw.print(",0");
            } else {
                System.out.print("," + i);
                pw.print("," + i);
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

    boolean inArticle;

    String paper;
    boolean isTheExpressArticle;
    boolean isDailyMailOrMailOnSundayArticle;
    boolean isDailyMirrorArticle;
    boolean isGuardianArticle;
    boolean isTelegraphArticle;
    boolean isMENArticle;
    boolean isBENArticle;
    boolean isTheEveningStandardArticle;

    /**
     * Iteratively parse through nodes.
     *
     * @param node
     */
    boolean isArticleNode(Node node) {
        //System.out.println(node.toString());
        String nodeName;
        nodeName = node.nodeName();
        //System.out.println("nodeName"+ nodeName);
        int nodeAttributeIndex;
        Attributes nodeAttributes;
        Iterator<Attribute> iteA;
        Attribute nodeAttribute;
        String key;
        String value;
        nodeAttributeIndex = 0;
        nodeAttributes = node.attributes();
        iteA = nodeAttributes.iterator();
        while (iteA.hasNext()) {
            //System.out.println("nodeAttributeIndex"+ nodeAttributeIndex);
            nodeAttribute = iteA.next();
            key = nodeAttribute.getKey();
            value = nodeAttribute.getValue();
            //System.out.println("key"+ key);
            //System.out.println("value"+ value);
            if (papers.contains(value)) {
                //System.out.println(value);
                paper = value;
                isTheExpressArticle = value.equalsIgnoreCase(sTheExpress);
                isDailyMailOrMailOnSundayArticle = value.equalsIgnoreCase(sDailyMail)
                        || value.equalsIgnoreCase(sMailOnSunday);
                isDailyMirrorArticle = value.equalsIgnoreCase(sDailyMirror);
                isGuardianArticle = value.equalsIgnoreCase(sTheGuardian);
                isTelegraphArticle = value.equalsIgnoreCase(sTheDailyTelegraph);
                isBENArticle = value.equalsIgnoreCase(sBEN);
                isMENArticle = value.equalsIgnoreCase(sMEN);
                isTheEveningStandardArticle = value.equalsIgnoreCase(sTheEveningStandard);
                return true;
                //parseExpressNode(node);
            }
            nodeAttributeIndex++;
        }
        return false;
    }

    String Date;
    boolean gotDate;
    int returnCount;

    /**
     * @param node
     */
    boolean getDate(Node node) {
        //System.out.println(node.toString());
        String nodeName;
        nodeName = node.nodeName();
        //System.out.println("nodeName"+ nodeName);
        int nodeAttributeIndex;
        Attributes nodeAttributes;
        Iterator<Attribute> iteA;
        Attribute nodeAttribute;
        String key;
        String value;
        nodeAttributeIndex = 0;
        nodeAttributes = node.attributes();
        iteA = nodeAttributes.iterator();
        while (iteA.hasNext()) {
            //System.out.println("nodeAttributeIndex"+ nodeAttributeIndex);
            nodeAttribute = iteA.next();
            key = nodeAttribute.getKey();
            value = nodeAttribute.getValue();
            //System.out.println("key"+ key);
            //System.out.println("value"+ value);
            if (key.equalsIgnoreCase("#text")) {
                if (!value.equalsIgnoreCase("\n")) {
                    Date += value;
                    if (isGuardianArticle) {
                        if (value.endsWith("GMT")) {
                            return true;
                        }
                    } else {
                        if (value.endsWith("day")) {
                            return true;
                        }
                    }
                }
            }
            nodeAttributeIndex++;
        }
        return false;
    }

    String Title;
    boolean startTitle;
    boolean gotTitle;

    /**
     * @param node
     */
    boolean getTitle(Node node) {
        //System.out.println(node.toString());
        String nodeName;
        nodeName = node.nodeName();
        //System.out.println("nodeName"+ nodeName);
        int nodeAttributeIndex;
        Attributes nodeAttributes;
        Iterator<Attribute> iteA;
        Attribute nodeAttribute;
        String key;
        String value;
        nodeAttributeIndex = 0;
        nodeAttributes = node.attributes();
        iteA = nodeAttributes.iterator();
        while (iteA.hasNext()) {
            //System.out.println("nodeAttributeIndex"+ nodeAttributeIndex);
            nodeAttribute = iteA.next();
            key = nodeAttribute.getKey();
            value = nodeAttribute.getValue();
            //System.out.println("key"+ key);
            //System.out.println("value"+ value);
            if (!startTitle) {
                if (value.equalsIgnoreCase("c7")) {
                    startTitle = true;
                }
            } else {
                if (key.equalsIgnoreCase("#text")) {
                    /**
                     * Replace all non alphabetical non numeric characters with
                     * a space. This is to help overcome issues with searching
                     * for terms that might be found in other words. It is not a
                     * perfect solution as some terms made up of several words
                     * might fall across two sentences and not really be terms
                     * at all, but just a set of words in the same order (e.g.
                     * instead of counting"migrant crisis this might count"...
                     * migrant. Crisis ...").
                     */
                    //value = value.replaceAll("[^A-Za-z0-9]"," ");
                    Title += value;
                }
                if (value.equalsIgnoreCase("c6")) {
                    // Remove double spaces
                    while (Title.contains("  ")) {
                        Title = Title.replaceAll("  ", " ");
                    }
                    return true;
                }
            }
            nodeAttributeIndex++;
        }
        return false;
    }

    String Section;
    boolean startSection;
    boolean gotSection;

    /**
     * Iteratively parse through nodes.
     *
     * @param node
     */
    boolean getSection(Node node) {
        //System.out.println("Node"+ node.toString());
        String nodeName;
        nodeName = node.nodeName();
        //System.out.println("nodeName"+ nodeName);
        int nodeAttributeIndex;
        Attributes nodeAttributes;
        Iterator<Attribute> iteA;
        Attribute nodeAttribute;
        String key;
        String value;
        nodeAttributeIndex = 0;
        nodeAttributes = node.attributes();
        iteA = nodeAttributes.iterator();
        while (iteA.hasNext()) {
            //System.out.println("nodeAttributeIndex"+ nodeAttributeIndex);
            nodeAttribute = iteA.next();
            key = nodeAttribute.getKey();
            value = nodeAttribute.getValue();
            //System.out.println("key"+ key);
            //System.out.println("value"+ value);
            if (!startSection) {
                if (value.equalsIgnoreCase("SECTION: ")) {
                    startSection = true;
                }
            } else {
                if (key.equalsIgnoreCase("#text")) {
                    Section += value;
                    return true;
                }
            }
            nodeAttributeIndex++;
        }
        return false;
    }

    String Length;
    boolean startLength;
    boolean gotLength;

    /**
     * Iteratively parse through nodes.
     *
     * @param node
     */
    boolean getLength(Node node) {
        //System.out.println("Node"+ node.toString());
        String nodeName;
        nodeName = node.nodeName();
        //System.out.println("nodeName"+ nodeName);
        int nodeAttributeIndex;
        Attributes nodeAttributes;
        Iterator<Attribute> iteA;
        Attribute nodeAttribute;
        String key;
        String value;
        nodeAttributeIndex = 0;
        nodeAttributes = node.attributes();
        iteA = nodeAttributes.iterator();
        while (iteA.hasNext()) {
            //System.out.println("nodeAttributeIndex"+ nodeAttributeIndex);
            nodeAttribute = iteA.next();
            key = nodeAttribute.getKey();
            value = nodeAttribute.getValue();
            //System.out.println("key"+ key);
            //System.out.println("value"+ value);
            if (!startLength) {
                if (value.equalsIgnoreCase("LENGTH: ")) {
                    startLength = true;
                }
            } else {
                if (key.equalsIgnoreCase("#text")) {
                    Length += value;
                    return true;
                }
            }
            nodeAttributeIndex++;
        }
        return false;
    }

    String Article;
    boolean startArticle;
    boolean gotArticle;

    /**
     * Iteratively parse through nodes.
     *
     * @param node
     */
    boolean getArticle(Node node) {
        //System.out.println("Node"+ node.toString());
        if (node.toString().equalsIgnoreCase("LOAD-DATE ")) {
            return true;
        }
        String nodeName;
        nodeName = node.nodeName();
        //System.out.println("nodeName"+ nodeName);
        // end at div
        int nodeAttributeIndex;
        Attributes nodeAttributes;
        Iterator<Attribute> iteA;
        Attribute nodeAttribute;
        String key;
        String value;

//        if (node.childNodeSize() > 0) {
//            List<Node> childNodes;
//            childNodes = node.childNodes();
//            Node childNode;
//            Iterator<Node> ite;
//            ite = childNodes.iterator();
//            while (ite.hasNext()) {
//                childNode = ite.next();
//                System.out.println("Node"+ childNode.toString());
////                if (childNode.toString().equalsIgnoreCase("SECTION: ")) {
////                    startSection = true;
////                }
//                nodeAttributeIndex = 0;
//                nodeAttributes = node.attributes();
//                iteA = nodeAttributes.iterator();
//                while (iteA.hasNext()) {
//                    System.out.println("nodeAttributeIndex"+ nodeAttributeIndex);
//                    nodeAttribute = iteA.next();
//                    key = nodeAttribute.getKey();
//                    value = nodeAttribute.getValue();
//                    System.out.println("key"+ key);
//                    System.out.println("value"+ value);
//                    if (!startLength) {
//                        if (value.equalsIgnoreCase("c7")) {
//                            startLength = true;
//                        }
//                    } else {
//                        if (key.equalsIgnoreCase("#text")) {
//                            Length += value;
//                            //if (value.endsWith("day")) {
//                            //    return true;
//                            //}
//                        }
////                if (value.equalsIgnoreCase("c6")) {
////                    return true;
////                }
//                    }
//                    nodeAttributeIndex++;
//                }
//
//            }
//        }
        nodeAttributeIndex = 0;
        nodeAttributes = node.attributes();
        iteA = nodeAttributes.iterator();
        while (iteA.hasNext()) {
            //System.out.println("nodeAttributeIndex"+ nodeAttributeIndex);
            nodeAttribute = iteA.next();
            key = nodeAttribute.getKey();
            value = nodeAttribute.getValue();
            //System.out.println("key"+ key);
            //System.out.println("value"+ value);
            if (key.equalsIgnoreCase("#text")) {
                if (!value.equalsIgnoreCase("\n")) {
                    if (value.equalsIgnoreCase("LOAD-DATE: ")) {
                        // Remove double spaces.
                        while (Article.contains("  ")) {
                            Article = Article.replaceAll("  ", " ");
                        }
                        return true;
                    }
                    /**
                     * Replace all non alphabetical non numeric characters with
                     * a space. This is to help overcome issues with searching
                     * for terms that might be found in other words. It is not a
                     * perfect solution as some terms made up of several words
                     * might fall across two sentences and not really be terms
                     * at all, but just a set of words in the same order (e.g.
                     * instead of counting"migrant crisis this might count"...
                     * migrant. Crisis ...").
                     */
                    //value = value.replaceAll("[^A-Za-z0-9]"," ");
                    /**
                     * Add space before punctuation and replace quotation marks
                     * with spaces.
                     */
                    value = value.replaceAll("\\'", " ");
                    value = value.replaceAll("\"", " ");
                    value = value.replaceAll("\'", " ");
                    value = value.replaceAll("\\.", " .");
                    value = value.replaceAll("\\?", " ?");
                    value = value.replaceAll("\\!", " !");
                    value = value.replaceAll("\\,", " ,");
                    value = value.replaceAll("\\;", " ;");
                    value = value.replaceAll("\\:", " :");
                    Article += value + " ";
                }
                //return true;
                //if (value.endsWith("day")) {
                //    return true;
                //}
            }
//                if (value.equalsIgnoreCase("c6")) {
//                    return true;
//                }
            nodeAttributeIndex++;
        }
        return false;
    }

    void parseChildNodes(Node node) {
        if (node.childNodeSize() > 0) {
            List<Node> childNodes;
            childNodes = node.childNodes();
            Node childNode;
            Iterator<Node> ite;
            ite = childNodes.iterator();
            while (ite.hasNext()) {
                childNode = ite.next();
                isArticleNode(childNode);
            }
        }
    }

    /**
     * This method parses the HTML file and returns results that are packed into
     * an Object[] result of size 5. result[0] is an int[] containing counts of
     * the numbers of mentions of each terms. result[1] is an int[] containing
     * counts of the numbers of articles that mentions of each terms. result[2]
     * is TreeMap with keys that are the DayOfWeek and values that are counts of
     * the number of times each term appears in those days articles. result[3]
     * is TreeMap with keys that are the DayOfWeek and values that are counts of
     * the number of articles each term appears in those days. result[4] is a
     * TreeSet of DateHeadlines which provides the dates and headlines of those
     * articles that mention headlineTerm in them.
     *
     * @param n
     * @param startDate
     * @param endDate
     * @param allterms
     * @param input The input file to be parsed.
     * @return
     */
    public Object[] parseHTML(int n, LocalDate startDate, LocalDate endDate,
            TreeMap<Integer, ArrayList<String>> allterms, File input) {
        inArticle = false;
        gotDate = false;
        gotTitle = false;
        Object[] result = new Object[5];
        TreeSet<DateOutlineDetails> headlineTermDateHeadlines;
        headlineTermDateHeadlines = new TreeSet<>();
//        BufferedReader br;
//        br = Generic_IO.getBufferedReader(input);

        Document doc = null;
        try {
            doc = Jsoup.parse(input, "utf-8");
            //String title = doc.title();
            //System.out.println(title);

        } catch (IOException ex) {
            Logger.getLogger(Text_Processor2.class
                    .getName()).log(Level.SEVERE, null, ex);
        }

        ArrayList<String> terms;
        int[] totalTermCounts = new int[n];
        result[0] = totalTermCounts;
        int[] totalArticleCountsForTerms = new int[n];
        result[1] = totalArticleCountsForTerms;
        HashMap<String, Integer> termCounts = new HashMap<>();
        //int[] termCounts = new int[n];
        //int[] articleCountsForTerms = new int[n];
        TreeMap<String, TreeMap<DayOfWeek, Integer>> totalTermCountByDay;
        totalTermCountByDay = new TreeMap<>();
        TreeMap<String, TreeMap<DayOfWeek, Integer>> totalArticleCountForTermsByDay;
        totalArticleCountForTermsByDay = new TreeMap<>();
        int i;
        String term;
        Iterator<String> ite2;
        i = 0;
        Iterator<Integer> iteB;
        iteB = allterms.keySet().iterator();
        while (iteB.hasNext()) {
            terms = allterms.get(iteB.next());
            ite2 = terms.iterator();
            while (ite2.hasNext()) {
                term = ite2.next();
                totalTermCounts[i] = 0;
                totalArticleCountsForTerms[i] = 0;
                termCounts.put(term, 0);
                //termCounts[i] = 0;
                //articleCountsForTerms[i] = 0;
                i++;
                totalTermCountByDay.put(term, new TreeMap<>());
                totalArticleCountForTermsByDay.put(term, new TreeMap<>());
            }
        }

        Elements elements;
        Element element;
        Iterator<Element> ite;
        Attributes elementAttributes;
        Attribute elementAttribute;
        List<Node> nodes;
        Iterator<Node> iteN;
        Node node;
        Attributes nodeAttributes;
        Attribute nodeAttribute;
        Iterator<Attribute> iteA;
        String key;
        String value;

        int elementIndex = 0;
        int elementAttributeIndex;
        int nodeIndex;

        elements = doc.getAllElements();// work from here using jsoup
        //Elements links = doc.getElementsByTag("div");
        ite = elements.iterator();
        while (ite.hasNext()) {

            // if inExpress then do the next thing here...
            //System.out.println("elementIndex"+ elementIndex);
            element = ite.next();
            if (element.hasText()) {
                //System.out.println(element.wholeText()); // Prints out entire document
                //System.out.println(element.text()); // Prints out entire document
                //System.out.println(element.nodeName());
            }
            elementAttributeIndex = 0;
            elementAttributes = element.attributes();
            iteA = elementAttributes.iterator();
            while (iteA.hasNext()) {
                //System.out.println("elementAttributeIndex"+ elementAttributeIndex);
                nodeAttribute = iteA.next();
                key = nodeAttribute.getKey();
                value = nodeAttribute.getValue();
                //System.out.println("key"+ key);
                //System.out.println("value"+ value);
                elementAttributeIndex++;
            }
            nodeIndex = 0;
            nodes = element.childNodes();
            iteN = nodes.iterator();
            while (iteN.hasNext()) {
                //System.out.println("nodeIndex"+ nodeIndex);
                node = iteN.next();
                if (inArticle) {
                    if (gotDate) {
                        if (gotTitle) {
                            if (gotSection) {
                                if (gotLength) {
                                    gotArticle = getArticle(node);
                                } else {
                                    gotLength = getLength(node);
                                    Article = " "; // The space could be important.
                                }
                            } else {
                                if (isDailyMailOrMailOnSundayArticle) {
                                    gotSection = true;
                                } else {
                                    gotSection = getSection(node);
                                }
                                Length = "";
                            }
                        } else {
                            //System.out.println("Got Date");
                            gotTitle = getTitle(node);
                            Section = "";
                        }
                    } else {
                        //System.out.println("In Express Article");
                        gotDate = getDate(node);
                        Title = " ";  // The space could be important.
                    }
                } else {
                    inArticle = isArticleNode(node);
                    Date = "";
                }
                nodeIndex++;
            }
            if (gotArticle) {
                //System.out.println("Got everything needed to process article... Process and reset.");
                //System.out.println("Date" + Date);
                LocalDate ld = parseDate(Date);

                // Filter for a given time period               
                if (ld.isAfter(startDate) && ld.isBefore(endDate)) {
                    paperArticleCounts.put(paper,
                            paperArticleCounts.get(paper) + 1);
                    //System.out.println("Title" + Title);
                    //System.out.println("Section" + Section);
                    //System.out.println("Length" + Length);
                    //System.out.println("Article" + Article);
                    DayOfWeek day = ld.getDayOfWeek();
                    TreeMap<DayOfWeek, Integer> articleCountsByDayOfWeek;
                    articleCountsByDayOfWeek = paperArticleCountsByDayOfWeek.get(paper);

                    articleCountsByDayOfWeek.put(day,
                            articleCountsByDayOfWeek.get(day) + 1);
                    i = 0;
                    iteB = allterms.keySet().iterator();
                    while (iteB.hasNext()) {
                        terms = allterms.get(iteB.next());
                        ite2 = terms.iterator();
                        while (ite2.hasNext()) {
                            term = ite2.next();
                            int c0 = termCounts.get(term);
                            int c1 = getTermCount(term, Article);
                            termCounts.put(term, c0 + c1);
                            if (c1 > 0) {
                                totalTermCounts[i] += c1;
                                totalArticleCountsForTerms[i]++;
                                addToCount(totalArticleCountForTermsByDay.get(term), day, 1);
                            }
                            addToCount(totalTermCountByDay.get(term), day, c1);
                            i++;
                        }
                    }
                    /**
                     * Store DateHeadline's for those articles on Saturdays that
                     * contain the term headlineTerm.
                     */
                    if (termCounts.get(headlineTerm) != null) {
                        if (termCounts.get(headlineTerm) > 0) {
                            if (ld.getDayOfWeek().equals(DayOfWeek.SATURDAY)) {
//                        // Fire off to Guardian Open Data to try to get page number...
//                        // This is now done in agdt-web in uk.ac.leeds.ccg.andyt.web.guardian.GuardianGetPage
//                        // See 
//                        if (!isTheExpressArticle) {
//                            int code = 1;
//                            String title;
//                            title = Title.replaceAll(" ","-");
//                            title = title.replaceAll("\\.","");
//                            title = title.replaceAll(";","");
//                            title = title.replaceAll(":","");
//                            title = title.replaceAll("\\?","");
//                            title = title.replaceAll("!","");
//                            String url;
//                            url ="http://content.guardianapis.com/search"
//                                    + "?show-fields="
//                                    + "newspaperPageNumber%2C"
//                                    + "newspaperEditionDate&q="
//                                    + title +"&api-key="+ GuardianAPIKey;
//                        }
                                headlineTermDateHeadlines.add(new DateOutlineDetails(ld,
                                        Section, Length, Title));
                            }
                        }
                    }
                }
                Iterator<Integer> iteC;
                iteC = allterms.keySet().iterator();
                Iterator<String> ite3;
                while (iteC.hasNext()) {
                    terms = allterms.get(iteC.next());
                    ite3 = terms.iterator();
                    while (ite3.hasNext()) {
                        term = ite3.next();
                        termCounts.put(term, 0);
                    }
                }
//                for (i = 0; i < n; i++) {
//                    termCounts[i] = 0;
//                }
                inArticle = false;
                gotDate = false;
                startTitle = false;
                gotTitle = false;
                startSection = false;
                gotSection = false;
                startLength = false;
                gotLength = false;
                gotArticle = false;
            }
            elementIndex++;
        }
        result[2] = totalTermCountByDay;
        result[3] = totalArticleCountForTermsByDay;
        result[4] = headlineTermDateHeadlines;
        return result;
    }

    /**
     * Adds totalTermCountOnDay values to grandTotalTermCountOnDays values for
     * the same keys.
     *
     * @param totalTermCountOnDay
     * @param grandTotalTermCountOnDays
     */
    public void addToCount(
            TreeMap<DayOfWeek, Integer> totalTermCountOnDay,
            TreeMap<DayOfWeek, Integer> grandTotalTermCountOnDays) {
        DayOfWeek day;
        int i;
        Iterator<DayOfWeek> ite = totalTermCountOnDay.keySet().iterator();
        while (ite.hasNext()) {
            day = ite.next();
            if (grandTotalTermCountOnDays.containsKey(day)) {
                i = grandTotalTermCountOnDays.get(day);
                i += totalTermCountOnDay.get(day);
            } else {
                i = totalTermCountOnDay.get(day);
            }
            grandTotalTermCountOnDays.put(day, i);
        }
    }

    /**
     * Adds count to the day entry in termCount.
     *
     * @param termCount
     * @param day
     * @param count
     */
    public void addToCount(TreeMap<DayOfWeek, Integer> termCount,
            DayOfWeek day, int count) {
        int i;
        if (termCount.containsKey(day)) {
            i = termCount.get(day);
            i += count;
        } else {
            i = count;
        }
        termCount.put(day, i);
    }

    /**
     * A generalised method that counts the number of times term appears in
     * text. The term may actually be multiple terms separated by " OR ". These
     * are counted individually and summed. For individual term, terms the term
     * count is added to for the term and for those instances where it has a
     * capitalised first letter.
     *
     * @param term
     * @param text
     * @return
     */
    int getTermCount(String term, String text) {
        //String lowerCaseLine = Generic_String.getLowerCase(line);
        int result = 0;
        if (term.contains(" OR ")) {
            String[] split;
            split = term.split(" OR ");
            for (String split1 : split) {
                //result += lowerCaseLine.split(split1).length - 1;
                result += getTermCount0(split1, text);
            }
        } else {
            //result += lowerCaseLine.split(term).length - 1;
            result += getTermCount0(term, text);
        }
        return result;
    }

    /**
     *
     * @param term
     * @param text
     * @return A count of the number of times term appears in text. This
     * includes the number of times the term with a capitalised first letter
     * also appears.
     */
    int getTermCount0(String term, String text) {
        int result = 0;
        String s;
        /**
         * Try with a capital first letter (as terms at the start of a sentence
         * have capital first letters).
         */
        s = Generic_String.getCapitalFirstLetter(term);
        /**
         * Adding a space before and after the term to distinguish from words
         * that contain terms.
         */
        //s = " " + s + " ";
        result += text.split(s).length - 1;
        //s = " " + term + " ";
        s = term;
        result += text.split(s).length - 1;
        return result;
    }

    /**
     * For parsing a String into a LocalDate.
     *
     * @param line
     * @return
     */
    LocalDate parseDate(String s) {
        LocalDate result;
        String month;
        String dayOfMonth;
        String year;
        //String dayOfWeek;
        String[] split;
        split = s.split(", ");
        String[] split2;
        split2 = split[0].split(" ");
        month = split2[0];
        dayOfMonth = split2[1];
        split2 = split[1].split(" ");
        year = split2[0];
        //dayOfWeek = split2[1];
//        String stringDate = " ";
//        stringDate +="year"+ year;
//        stringDate +="month"+ month;
//        stringDate +="dayOfMonth"+ dayOfMonth;
//        stringDate +="dayOfWeek"+ dayOfWeek;
//        System.out.println(stringDate);
        Month m = Month.valueOf(month.toUpperCase());
        result = LocalDate.of(Integer.valueOf(year), m, Integer.valueOf(dayOfMonth));
        return result;
    }

    /**
     * Reads a Guardian API Key from an ASCII text file. N.B. This is currently
     * not used as a different program is used for processing using the Guardian
     * API.
     *
     * @return
     */
    String getGuardianAPIKey() throws FileNotFoundException, IOException {
        String r = "";
        File f;
        File dir  = new File(files.getDir().toFile(), "private");
        f = new File(dir, "GuardianAPIKey.txt");
        try (BufferedReader br = Generic_IO.getBufferedReader(f.toPath())) {
            try {
                r = br.readLine();
            } catch (IOException ex) {
                Logger.getLogger(Text_Processor2.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return r;
        
    }

    /**
     * A simple inner class for wrapping a LocalDate and a String such that
     * different instances can be ordered which the are first by Date and then
     * by the String.
     */
    public class DateOutlineDetails implements Comparable<DateOutlineDetails> {

        LocalDate LD;
        String Section;
        String Length;
        String Headline;

        public DateOutlineDetails() {
        }

        public DateOutlineDetails(
                LocalDate ld, String section, String length, String headline) {
            LD = ld;
            Section = section;
            Length = length;
            Headline = headline;
        }

        @Override
        public int compareTo(DateOutlineDetails t) {
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
                        int outlineDetailsComparison;
                        outlineDetailsComparison = (Section + Length + Headline).compareTo(
                                t.Headline + t.Length + t.Headline);
                        return outlineDetailsComparison;
                    }
                }
            } else {
                return dateComparison;
            }
        }
    }

    /**
     * Initialise terms. There are capitalisations and space that are important
     * in these terms. There are also terms separated by " OR ". For those terms
     * the article counts will just measure the the number of articles in the
     * selections where either term appears and will sum up the number of times
     * the terms appear in the articles.
     *
     * @TODO: This could all be read in from a file rather than being hard coded
     * here.
     */
    Object[] getAllTermsFelicity() {
        Object[] result;
        result = new Object[3];
        TreeMap<Integer, ArrayList<String>> allterms = new TreeMap<>();
        result[0] = allterms;
        HashMap<Integer, String> termTypes = new HashMap<>();
        result[1] = termTypes;
        int i = 0;
        int index = -1;
        // Key Migrant/Refugee Terms
        termTypes.put(i, "Key Migrant/Refugee Terms");
        ArrayList mrterms = new ArrayList();
        allterms.put(i, mrterms);
        i++;
        mrterms.add("asylum seeker");
        mrterms.add("economic migrant");
        mrterms.add("humanitarian crisis");
        mrterms.add("illegal immigrant");
        mrterms.add("illegal migrant");
        mrterms.add("immigration crisis");
        mrterms.add("migrant crisis");
        mrterms.add("migrant flood OR flood of migrants");
        mrterms.add("refugee crisis");
        mrterms.add("refugee camp");
        index += mrterms.size();
        // People Terms
        // Types of People Terms
        termTypes.put(i, "Types of People");
        ArrayList ptterms = new ArrayList();
        allterms.put(i, ptterms);
        i++;
        ptterms.add("alien");
        /**
         * WARNING: Although many of these terms are people from a country,
         * these terms are the same as those for other things belonging to the
         * place e.g. a Russian is a person from Russia and a Russian tank is a
         * tank from Russia.
         */
        ptterms.add("afghan");
        ptterms.add("british");
        ptterms.add("christian");
        ptterms.add("civilian");
        ptterms.add("english");
        ptterms.add("fellow human");
        ptterms.add("foreigner");
        ptterms.add("french");
        ptterms.add("greek");
        ptterms.add("human");
        ptterms.add("innocent");
        ptterms.add("intruder");
        ptterms.add("iraqi");
        ptterms.add("kurd OR kurdish");
        ptterms.add("lebanese");
        ptterms.add("migrant");
        ptterms.add("muslim");
        ptterms.add("people trafficker OR human trafficker");
        ptterms.add("rabid dog");
        ptterms.add("refugee");
        ptterms.add("smuggler OR smuggling");
        ptterms.add("stow away");
        ptterms.add("syrian");
        ptterms.add("terrorist");
        ptterms.add("yemeni");
        index += ptterms.size();
        // Individuals
        termTypes.put(i, "Individuals");
        ArrayList iterms = new ArrayList();
        allterms.put(i, iterms);
        i++;
        iterms.add("Alan Kurdi OR Aylan Kurdi"); // 3 year old boy washed up on beach (https://en.wikipedia.org/wiki/Death_of_Alan_Kurdi)
        iterms.add("Assad"); // Leader of Syria
        iterms.add("David Cameron OR Teresa May"); // UK Prime Ministers
        iterms.add("Erdoğan OR erdogan"); // Leader of Turkey
        iterms.add("Gutteres OR Grandi"); // Head of UNHCR
        iterms.add("Hammond OR Johnson OR foreign secretary"); // UK foreign secretaries
        iterms.add("Hollande"); // France Prime Minister
        iterms.add("Juncker"); // Head of EC
        iterms.add("Ki-Moon OR Ki Moon OR Annan"); // Head of UN
        iterms.add("Merkel"); // Germany Prime Minister
        iterms.add("Sturgeon"); // Leader of The SNP
        iterms.add("Nigel Farage"); // Leader of UKIP
        iterms.add("Putin"); // Russian president
        iterms.add("Steven Woolfe"); // UKIP Migration Spokesman and UK MEP
        index += iterms.size();
        // Organisations
        // Political
        termTypes.put(i, "Polictical Organisations");
        ArrayList poterms = new ArrayList();
        allterms.put(i, poterms);
        i++;
        poterms.add("EU Turkey Deal OR EU-Turkey Deal OR EU-Turkey deal OR deal between the EU and Turkey");
        poterms.add("EDL");
        poterms.add("far right");
        poterms.add("IS OR islamic state OR Islamic State");
        poterms.add("ISIL");
        poterms.add("ISIS");
        poterms.add("nazi");
        poterms.add("right wing");
        poterms.add("UKIP");
        index += poterms.size();
        // Relief Agencies
        termTypes.put(i, "Relief Agencies");
        ArrayList raterms = new ArrayList();
        allterms.put(i, raterms);
        i++;
        raterms.add("Amnesty International");
        raterms.add("Oxfam");
        raterms.add("UN");
        raterms.add("UNICEF");
        raterms.add("UNHCR");
        raterms.add("UNRWA");
        raterms.add("MSF");
        raterms.add("Red Cross");
        index += raterms.size();
        // Places
        // Large Regions
        termTypes.put(i, "Large Regions");
        ArrayList lrterms = new ArrayList();
        allterms.put(i, lrterms);
        i++;
        lrterms.add("Africa");
        lrterms.add("Asia");
        lrterms.add("EU");
        lrterms.add("Europe");
        lrterms.add("Mediterranean");
        index += lrterms.size();
        // Countries
        termTypes.put(i, "Countries");
        ArrayList cterms = new ArrayList();
        allterms.put(i, cterms);
        i++;
        cterms.add("Afghanistan");
        index++;
        /**
         * As with Egypt, some spaces are added here on purpose so as to not
         * count longer terms that start in the same way e.g. Egypt and
         * Egyptian.
         */
        cterms.add("Egypt ");
        index++;
        cterms.add("England OR Britain OR UK");
        index++;
        cterms.add("France");
        index++;
        cterms.add("Germany");
        index++;
        cterms.add("Greece");
        index++;
        cterms.add("Iraq ");
        index++;
        cterms.add("Israel ");
        index++;
        cterms.add("Italy");
        index++;
        cterms.add("Jordan ");
        index++;
        cterms.add("Lebanon");
        index++;
        cterms.add("Macedonia ");
        index++;
        cterms.add("Russia ");
        index++;
        cterms.add("Spain");
        index++;
        cterms.add("Syria ");
        index++;
        cterms.add("Turkey");
        index++;
        cterms.add("West Bank");
        index++;
        cterms.add("Yemen ");
        index++;
        // Regions in Countries
        termTypes.put(i, "Regions in Countries");
        ArrayList ricterms = new ArrayList();
        allterms.put(i, ricterms);
        i++;
        ricterms.add("Bodrum"); // Region of Turkey
        ricterms.add("Lesbos"); // Greek island
        index += ricterms.size();
        // Syrian Cities
        termTypes.put(i, "Syrian Cities");
        ArrayList scterms = new ArrayList();
        allterms.put(i, scterms);
        i++;
        scterms.add("Aleppo");
        scterms.add("Damascus");
        scterms.add("Homs");
        scterms.add("Raqqa");
        index += scterms.size();
        // Syrian Refugee Camps (see https://en.wikipedia.org/wiki/Syrian_refugee_camps)
        // Refugee Camps in Turkey
        termTypes.put(i, "Refugee Camps in Turkey");
        ArrayList rcitterms = new ArrayList();
        allterms.put(i, rcitterms);
        i++;
        rcitterms.add("Altınözü OR Altinozu");
        rcitterms.add("Yayladağı OR Yayladagi");
        rcitterms.add("apaydın");
        rcitterms.add("Güveççi OR Guvecci");
        rcitterms.add("Ceylanpınar");
        rcitterms.add("Akçakale OR Akcakale");
        rcitterms.add("Harran");
        rcitterms.add("Viranşehir OR Viransehir");
        rcitterms.add("Suruç OR Suruc");
        rcitterms.add("Islahiye");
        rcitterms.add("Karkamış OR Karkamis");
        rcitterms.add("Nizip");
        rcitterms.add("Öncüpınar OR Oncupinar");
        rcitterms.add("Elbeyli Besiriye");
        rcitterms.add("Merkez");
        rcitterms.add("Cevdetiye");
        rcitterms.add("Sarıçam OR Saricam");
        rcitterms.add("Midyat");
        rcitterms.add("Beydağı OR Beydagi");
        index += rcitterms.size();
        // Refugee Camps in Jordan
        termTypes.put(i, "Refugee Camps in Jordan");
        ArrayList rcijterms = new ArrayList();
        allterms.put(i, rcijterms);
        i++;
        rcijterms.add("Zaatari");
        rcijterms.add("Azraq");
        rcijterms.add("Mrajeeb Al Fhood");
        rcijterms.add("Rukban");
        rcijterms.add("Hadalat");
        index += rcijterms.size();
        // Refugee Camps in Iraq
        termTypes.put(i, "Refugee Camps in Iraq");
        ArrayList rciiterms = new ArrayList();
        allterms.put(i, rciiterms);
        i++;
        rciiterms.add("Domiz");
        rciiterms.add("Gawilan");
        rciiterms.add("Akre");
        rciiterms.add("Darashakran");
        rciiterms.add("Kawergosk");
        rciiterms.add("Qushtapa");
        rciiterms.add("Basirma");
        rciiterms.add("Arbat");
        rciiterms.add("Al-Obaidi OR Al Obaidi OR Alobaidi");
        index += rciiterms.size();
        // Refugee Camps in Macedonia
        termTypes.put(i, "Refugee Camps in Macedonia");
        ArrayList rcimterms = new ArrayList();
        allterms.put(i, rcimterms);
        i++;
        rcimterms.add("Gevgelija");
        rcimterms.add("Tabanovce");
        index += rcimterms.size();
        // Refugee Camps in Greece
        termTypes.put(i, "Refugee Camps in Greece");
        ArrayList rcigterms = new ArrayList();
        allterms.put(i, rcigterms);
        i++;
        rcigterms.add("Doliana");
        rcigterms.add("Katsika");
        rcigterms.add("Konitsa");
        rcigterms.add("Filippada");
        rcigterms.add("Tselepevo");
        rcigterms.add("Alexandreia");
        rcigterms.add("Cherso");
        rcigterms.add("Derveni Alexill");
        rcigterms.add("Eko");
        rcigterms.add("Diavata");
        rcigterms.add("Giannitsa");
        rcigterms.add("Idomeni");
        rcigterms.add("Kalochori OR Iliadi");
        rcigterms.add("Lagadika");
        rcigterms.add("Nea Kavala");
        rcigterms.add("Oraiokastro");
        rcigterms.add("Piera");
        rcigterms.add("Sinatex");
        rcigterms.add("Sindos");
        rcigterms.add("Softex");
        rcigterms.add("Thessaloniki");
        rcigterms.add("Vasilika");
        rcigterms.add("Veria");
        rcigterms.add("Viagiohori");
        rcigterms.add("Chalkero");
        rcigterms.add("Drama");
        rcigterms.add("Andravidas");
        rcigterms.add("Oiuofyta");
        rcigterms.add("Ritsona");
        rcigterms.add("Thermopiles");
        rcigterms.add("Kipselochori");
        rcigterms.add("Larissa");
        rcigterms.add("Volos");
        rcigterms.add("Agios Andreas");
        rcigterms.add("Elefsina");
        rcigterms.add("Eleonas");
        rcigterms.add("Elliniko");
        rcigterms.add("Lavrio");
        rcigterms.add("Malakasa");
        rcigterms.add("Piraeus");
        rcigterms.add("Schisto");
        rcigterms.add("Skaramagas");
        rcigterms.add("Victoria Square");
        rcigterms.add("Moria");
        rcigterms.add("Chios");
        rcigterms.add("Vial");
        rcigterms.add("Vathy");
        rcigterms.add("Leros");
        rcigterms.add("Lepida");
        rcigterms.add("Kos");
        rcigterms.add("Rhodes");
        index += rcigterms.size();
        // Other Camp Terms
        termTypes.put(i, "Other Camp Terms");
        ArrayList octterms = new ArrayList();
        allterms.put(i, octterms);
        i++;
        octterms.add("calais");
        octterms.add("camp");
        octterms.add("container camp");
        octterms.add("jungle OR Jules Ferry"); // https://en.wikipedia.org/wiki/Calais_Jungle https://en.wikipedia.org/wiki/Migrants_around_Calais
        octterms.add("tent camp");
        index += octterms.size();

        // Other Key Terms
        // Border Movement Terms
        termTypes.put(i, "Border Movement Terms");
        ArrayList bmterms = new ArrayList();
        allterms.put(i, bmterms);
        i++;
        bmterms.add("across the border OR over the border");
        bmterms.add("border");
        bmterms.add("crossing");
        bmterms.add("boat");
        bmterms.add("displacement");
        bmterms.add("exodus");
        bmterms.add("fence OR wall");
        bmterms.add("flee");
        bmterms.add("lorry OR lorries");
        bmterms.add("movement");
        index += bmterms.size();
        // Movement Scale Descriptors
        termTypes.put(i, "Movement Scale Descriptors");
        ArrayList msdterms = new ArrayList();
        allterms.put(i, msdterms);
        i++;
        msdterms.add("dam has burst");
        msdterms.add("flee");
        msdterms.add("flood");
        msdterms.add("flow");
        msdterms.add("influx");
        msdterms.add("swarm");
        msdterms.add("torrent");
        msdterms.add("engulf");
        msdterms.add("storm");
        msdterms.add("surge");
        msdterms.add("swell");
        msdterms.add("swamp");
        msdterms.add("tide");
        msdterms.add("wave");
        index += msdterms.size();
        // Bogus Asylum Related Terms 
        termTypes.put(i, "Bogus Asylum Related Terms");
        ArrayList barterms = new ArrayList();
        allterms.put(i, barterms);
        i++;
        barterms.add("bogus");
        barterms.add("bogus asylum seeker");
        barterms.add("failed asylum seeker");
        barterms.add("false asylum claim");
        barterms.add("bogus asylum claim");
        index += barterms.size();
        // Miscellaneous Key Terms
        termTypes.put(i, "Miscellaneous Key Terms");
        ArrayList mktterms = new ArrayList();
        allterms.put(i, mktterms);
        i++;
        mktterms.add("aid");
        mktterms.add("asylum");
        mktterms.add("attack");
        mktterms.add("brexit");
        mktterms.add("brexit OR EU Referendum OR European Union Referendum");
        mktterms.add("bremain OR remainer");
        mktterms.add("burden");
        mktterms.add("child OR children");
        mktterms.add("chaos");
        mktterms.add("crime");
        mktterms.add("crisis");
        mktterms.add("crusade");
        mktterms.add("deal");
        mktterms.add("desperate OR desparation");
        mktterms.add("drown OR drowning");
        mktterms.add("dying OR dies OR death");
        mktterms.add("disease");
        mktterms.add("extremism");
        mktterms.add("economic");
        mktterms.add("famine");
        mktterms.add("foreign");
        mktterms.add("genocide");
        mktterms.add("hate crime");
        mktterms.add("hunger OR malnutrition OR malnourished OR starving");
        mktterms.add("illegal");
        mktterms.add("immigration");
        mktterms.add("imperialism");
        mktterms.add("islam ");
        mktterms.add("islamophobia");
        mktterms.add("mop OR stem the flow");
        mktterms.add("moral");
        mktterms.add("moral imperialism");
        mktterms.add("open borders OR open border policy");
        mktterms.add("nationalism OR nation");
        mktterms.add("plight");
        mktterms.add("relief");
        mktterms.add("referendum");
        mktterms.add("resettlement");
        mktterms.add("security");
        mktterms.add("subside");
        mktterms.add("threat");
        mktterms.add("threat to security");
        mktterms.add("terror ");
        mktterms.add("terrorism");
        mktterms.add("tragedy OR tragic");
        mktterms.add("trafficking");
        mktterms.add("work");
        mktterms.add("war");
        index += mktterms.size();
        // Headline terms
        if (writeHeadlines) {
            termTypes.put(i, "Miscellaneous Key Terms");
            ArrayList hterms = new ArrayList();
            allterms.put(i, hterms);
            i++;
            hterms.add(headlineTerm);
            index += hterms.size();
        }
        int numberOfTerms = index + 1;
        result[2] = numberOfTerms;
        return result;
    }

    /**
     * Initialise terms. There are capitalisations and space that are important
     * in these terms. There are also terms separated by " OR ". For those terms
     * the article counts will just measure the the number of articles in the
     * selections where either term appears and will sum up the number of times
     * the terms appear in the articles.
     *
     * @TODO: This could all be read in from a file rather than being hard coded
     * here.
     */
    Object[] getAllTermsEmma() {
        Object[] result;
        result = new Object[3];
        TreeMap<Integer, ArrayList<String>> allterms = new TreeMap<>();
        result[0] = allterms;
        HashMap<Integer, String> termTypes = new HashMap<>();
        result[1] = termTypes;
        int i = 0;
        int index = -1;
        // Key Migrant/Refugee Terms
        termTypes.put(i, "Key Migrant/Refugee Terms");
        ArrayList mrterms = new ArrayList();
        allterms.put(i, mrterms);
        i++;
        mrterms.add("asylum seeker");
        mrterms.add("economic migrant");
        mrterms.add("humanitarian crisis");
        mrterms.add("illegal immigrant");
        mrterms.add("illegal migrant");
        mrterms.add("immigration crisis");
        mrterms.add("migrant crisis");
        mrterms.add("migrant flood OR flood of migrants");
        mrterms.add("refugee crisis");
        mrterms.add("refugee camp");
        index += mrterms.size();
        // People Terms
        // Types of People Terms
        /**
         * WARNING: Although many of these terms are people from a country,
         * these terms are the same as those for other things belonging to the
         * place e.g. a Russian is a person from Russia and a Russian tank is a
         * tank from Russia.
         */
        termTypes.put(i, "Types of People");
        ArrayList ptterms = new ArrayList();
        allterms.put(i, ptterms);
        i++;
        ptterms.add("afghan");
        ptterms.add("alien");
        ptterms.add("british");
        ptterms.add("christian");
        ptterms.add("civilian");
        ptterms.add("english");
        ptterms.add("fellow human");
        ptterms.add("foreigner");
        ptterms.add("french");
        ptterms.add("greek");
        ptterms.add("human");
        ptterms.add("innocent");
        ptterms.add("intruder");
        ptterms.add("iraqi");
        ptterms.add("kurd  OR kurdish");
        ptterms.add("lebanese");
        ptterms.add("migrant");
        ptterms.add("muslim");
        ptterms.add("people trafficker OR human trafficker");
        ptterms.add("rabid dog");
        ptterms.add("refugee");
        ptterms.add("smuggler OR smuggling");
        ptterms.add("stow away");
        ptterms.add("syrian");
        ptterms.add("terrorist");
        ptterms.add("yemeni");
        index += ptterms.size();
        // Individuals
        termTypes.put(i, "Individuals");
        ArrayList iterms = new ArrayList();
        allterms.put(i, iterms);
        i++;
        iterms.add("Alan Kurdi OR Aylan Kurdi"); // 3 year old boy washed up on beach (https://en.wikipedia.org/wiki/Death_of_Alan_Kurdi)
        iterms.add("Assad"); // Leader of Syria
        iterms.add("Cox"); // Murdered MP: Jo Cox
        iterms.add("David Cameron OR Teresa May"); // UK Prime Ministers
        iterms.add("Erdoğan OR Erdogan"); // Leader of Turkey
        iterms.add("Gutteres OR Grandi"); // Head of UNHCR
        iterms.add("Hammond OR Johnson OR foreign secretary"); // UK foreign secretaries
        iterms.add("Hollande"); // France Prime Minister
        iterms.add("Juncker"); // Head of EC
        iterms.add("Ki-Moon OR Ki Moon OR Annan"); // Head of UN
        iterms.add("Merkel"); // Germany Prime Minister
        iterms.add("Sturgeon"); // Leader of The SNP: Nicola Sturgeon
        iterms.add("Farage"); // Leader of UKIP: Nigel Farage
        iterms.add("Putin"); // Russian president: Vladamir Putin
        iterms.add("Woolfe"); // UKIP Migration Spokesman and UK MEP: Steven Woolfe
        index += iterms.size();
        // Organisations
        // Political
        termTypes.put(i, "Political Terms (mostly organisations)");
        ArrayList poterms = new ArrayList();
        allterms.put(i, poterms);
        i++;
        poterms.add("brexiteer");
        poterms.add("BNP OR British National Party");
        poterms.add("EDL");
        poterms.add("EU Turkey Deal OR EU-Turkey Deal OR EU-Turkey deal OR deal between the EU and Turkey");
        poterms.add("EU Referendum OR EU referendum");
        poterms.add("Conservative");
        poterms.add("conservative");
        poterms.add("far right");
        poterms.add("Green Party");
        poterms.add("IS OR islamic state OR Islamic State");
        poterms.add("ISIL");
        poterms.add("ISIS");
        poterms.add("Labour");
        poterms.add("labour");
        poterms.add("Leave");
        poterms.add("leave");
        poterms.add("left wing");
        poterms.add("left wing OR liberal");
        poterms.add("Liberal");
        poterms.add("liberal");
        poterms.add("nazi");
        poterms.add("Remain");
        poterms.add("remain");
        poterms.add("right wing");
        poterms.add("SNP OR Scottish National Party");
        poterms.add("UKIP");
        index += poterms.size();

        // Other Camp Terms
        termTypes.put(i, "Other Camp Terms");
        ArrayList octterms = new ArrayList();
        allterms.put(i, octterms);
        i++;
        octterms.add("calais");
        octterms.add("camp");
        octterms.add("container camp");
        octterms.add("jungle OR Jules Ferry"); // https://en.wikipedia.org/wiki/Calais_Jungle https://en.wikipedia.org/wiki/Migrants_around_Calais
        octterms.add("tent camp");
        index += octterms.size();

        // Other Key Terms
        // Border Movement Terms
        termTypes.put(i, "Border Movement Terms");
        ArrayList bmterms = new ArrayList();
        allterms.put(i, bmterms);
        i++;
        bmterms.add("across the border OR over the border");
        bmterms.add("border");
        bmterms.add("crossing");
        bmterms.add("boat");
        bmterms.add("displacement");
        bmterms.add("exodus");
        bmterms.add("fence OR wall");
        bmterms.add("flee");
        bmterms.add("lorry OR lorries");
        bmterms.add("movement");
        index += bmterms.size();
        // Movement Scale Descriptors
        termTypes.put(i, "Movement Scale Descriptors");
        ArrayList msdterms = new ArrayList();
        allterms.put(i, msdterms);
        i++;
        msdterms.add("dam has burst");
        msdterms.add("flee");
        msdterms.add("flood");
        msdterms.add("flow");
        msdterms.add("influx");
        msdterms.add("swarm");
        msdterms.add("torrent");
        msdterms.add("engulf");
        msdterms.add("storm");
        msdterms.add("surge");
        msdterms.add("swell");
        msdterms.add("swamp");
        msdterms.add("tide");
        msdterms.add("wave");
        index += msdterms.size();
        // Bogus Asylum Related Terms 
        termTypes.put(i, "Bogus Asylum Related Terms");
        ArrayList barterms = new ArrayList();
        allterms.put(i, barterms);
        i++;
        barterms.add("bogus");
        barterms.add("bogus asylum seeker");
        barterms.add("failed asylum seeker");
        barterms.add("false asylum claim");
        barterms.add("bogus asylum claim");
        index += barterms.size();
        // Miscellaneous Key Terms
        termTypes.put(i, "Miscellaneous Key Terms");
        ArrayList mktterms = new ArrayList();
        allterms.put(i, mktterms);
        i++;
        mktterms.add("aid");
        mktterms.add("asylum");
        mktterms.add("attack");
        mktterms.add("brexit");
        mktterms.add("British culture");
        mktterms.add("burden");
        mktterms.add("child OR children");
        mktterms.add("chaos");
        mktterms.add("community");
        mktterms.add("crime");
        mktterms.add("crisis");
        mktterms.add("deal");
        mktterms.add("desperate OR desparation");
        mktterms.add("drown OR drowning");
        mktterms.add("dying OR dies OR death");
        mktterms.add("disease");
        mktterms.add("extremism");
        mktterms.add("economic");
        mktterms.add("famine");
        mktterms.add("foreign");
        mktterms.add("genocide");
        mktterms.add("housing");
        mktterms.add("hate crime");
        mktterms.add("hunger OR malnutrition OR malnourished OR starving");
        mktterms.add("illegal");
        mktterms.add("immigration");
        mktterms.add("imperialism");
        mktterms.add("islam ");
        mktterms.add("islamophobia");
        mktterms.add("mop OR stem the flow");
        mktterms.add("moral");
        mktterms.add("moral imperialism");
        mktterms.add("NHS");
        mktterms.add("open borders OR open border policy");
        mktterms.add("nationalism OR nation");
        mktterms.add("plight");
        mktterms.add("relief");
        mktterms.add("resettlement");
        mktterms.add("security");
        mktterms.add("subside");
        mktterms.add("threat");
        mktterms.add("threat to security");
        mktterms.add("terror ");
        mktterms.add("terrorism");
        mktterms.add("tragedy OR tragic");
        mktterms.add("trafficking");
        mktterms.add("work");
        mktterms.add("war");
        index += mktterms.size();
        // Headline terms
        if (writeHeadlines) {
            termTypes.put(i, "Miscellaneous Key Terms");
            ArrayList hterms = new ArrayList();
            allterms.put(i, hterms);
            i++;
            hterms.add(headlineTerm);
            index += hterms.size();
        }
        int numberOfTerms = index + 1;
        result[2] = numberOfTerms;
        return result;
    }

    /**
     * Initialise terms. There are capitalisations and space that are important
     * in these terms. There are also terms separated by " OR ". For those terms
     * the article counts will just measure the the number of articles in the
     * selections where either term appears and will sum up the number of times
     * the terms appear in the articles.
     *
     * @TODO: This could all be read in from a file rather than being hard coded
     * here.
     */
    Object[] getAllTermsHarriet() {
        Object[] result;
        result = new Object[3];
        TreeMap<Integer, ArrayList<String>> allterms = new TreeMap<>();
        result[0] = allterms;
        HashMap<Integer, String> termTypes = new HashMap<>();
        result[1] = termTypes;
        int i = 0;
        int index = -1;
        // Key Terms
        termTypes.put(i, "Key Terms");
        ArrayList kterms = new ArrayList();
        allterms.put(i, kterms);
        i++;
        kterms.add("hate crime");
        kterms.add("racist OR racism");
        kterms.add("muslim");
        kterms.add("islamophobia");
        index += kterms.size();
        // Headline terms
        if (writeHeadlines) {
            termTypes.put(i, "Miscellaneous Key Terms");
            ArrayList hterms = new ArrayList();
            allterms.put(i, hterms);
            i++;
            hterms.add(headlineTerm);
            index += hterms.size();
        }
        int numberOfTerms = index + 1;
        result[2] = numberOfTerms;
        return result;
    }
}
