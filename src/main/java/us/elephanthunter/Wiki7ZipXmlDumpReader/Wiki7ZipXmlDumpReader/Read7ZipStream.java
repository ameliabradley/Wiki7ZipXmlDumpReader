package us.elephanthunter.Wiki7ZipXmlDumpReader.Wiki7ZipXmlDumpReader;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.Thread;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLInputFactory;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.ArrayList;

public class Read7ZipStream {

    public synchronized static void printProgress (double totalBytes, double currentBytes, String strNote) {
        // Example output:
        //System.out.print("71% [===========================>           ] 358,756,352 51.2M/s  eta 3s");

        double totalTicks = 40;
        double iRatio = currentBytes / totalBytes;
        double ticksToShow = Math.floor(totalTicks * iRatio);
        double iSpacesToShow = totalTicks - ticksToShow;

        double iPercent = iRatio * 100;
        DecimalFormat df = new DecimalFormat("0.0000000");
        System.err.print("\r");
        System.err.print(df.format(iPercent) + "% [");
        for (int i = 0; i < ticksToShow; i++) {
            System.err.print("=");
        }

        for (int i = 0; i < iSpacesToShow; i++) {
            System.err.print(" ");
        }

        System.err.print("]  ");
        System.err.print(strNote);
    }

    /**
     * Implementation of string join for old versions of Java
     * @param separator (ex: ",")
     * @param mList (ex: ["A", "B", "TURK3Y"])
     * @return the concatenated string... (ex: "A,B,TURK3Y")
     */
    public static String join(String separator, ArrayList<String> mList) {
        StringBuilder sb = new StringBuilder();
        int count = 0;

        for (String m: mList) {
            sb.append(m);
            count++;
            if (count < mList.size()) {
                sb.append(separator);
            }
        }

        return sb.toString();
    }

    public static void main(String[] args) {
        if(args.length < 1) {
          System.err.println("Must provide a valid file path as the first parameter; no argument given.");
          System.exit(1);
        }
        if(args[0].length() < 1) {
          System.err.println("Must provide a valid file path as the first parameter; empty argument given.");
          System.exit(1);
        }
        // Example files:
        // enwiki-20160501-pages-meta-history5.xml-p000564697p000565313.7z
        // enwiki-20160501-pages-meta-history27.xml-p042663462p043423161.7z
        try {
            File zFile = new File(args[0]);
            if( ! zFile.exists()) {
                System.err.println("The file specified by the first parameter must exist, but does not.");
                System.exit(1);
            }

            if( ! zFile.canRead()) {
                System.err.println("The file specified by the first parameter must be readable, but can't be read.");
                System.exit(1);
            }

            SevenZFile sevenZFile = new SevenZFile(zFile);

            final SevenZFileInputStream sevenZFileInputStream = new SevenZFileInputStream(sevenZFile);

            SevenZArchiveEntry tmpEntry = sevenZFile.getNextEntry();

            Thread myThread;

            while(tmpEntry!=null){
                System.err.print("File Size: ");
                System.err.println(tmpEntry.getSize());
                if(tmpEntry == null) break;

                try {
                    final XMLInputFactory factory = XMLInputFactory.newInstance();
                    final XMLStreamReader reader = factory.createXMLStreamReader(sevenZFileInputStream);
                    final SevenZArchiveEntry entry = tmpEntry;

                    myThread = new Thread(new Runnable() {

                        @Override
                        public void run() {
                            long iTotalBytes = entry.getSize();

                            String note = "";
                            int nestingLevel = 0;
                            ArrayList<String> nestedZerglings = new ArrayList<String>();

                            try {
                                Date lastDate = new Date();
                                Date  currentDate;
                                String elementName;

                                while (reader.hasNext()) {
                                    int eventType = reader.next();
                                    switch (eventType) {
                                        case XMLStreamReader.START_ELEMENT:
                                            elementName = reader.getLocalName();
                                            // Hack to get around XML parsing bug (i.e. reading next element start before prev element end)
                                            if (nestedZerglings.size() < 1 || ! nestedZerglings.get(nestedZerglings.size() - 1).equals(elementName))
                                            {
                                                nestingLevel++;
                                                note = elementName;
                                                nestedZerglings.add(elementName);

                                                if (join("/", nestedZerglings).equals("mediawiki/page/revision/id")) {
                                                    String revisionId = reader.getElementText();
                                                    System.out.println(revisionId);
                                                }
                                            } else {
                                                System.err.println("Skipping element (start): " + elementName);
                                            }
                                            break;
                                        case XMLStreamReader.END_ELEMENT:
                                            elementName = reader.getLocalName();
                                            while(nestedZerglings.size() > 0 && nestedZerglings.contains(elementName)) {
                                                nestedZerglings.remove(nestedZerglings.size() - 1);
                                                nestingLevel--;

                                                if ((nestedZerglings.size()) != nestingLevel) {
                                                    System.err.println("Wat");
                                                    return;
                                                }
                                            }
                                            break;
                                    }

                                    currentDate = new Date();
                                    if(currentDate.getTime() - lastDate.getTime() > 500) { // 5 seconds
                                        note = nestingLevel + " " + join("/", nestedZerglings);
                                        //printProgress(iTotalBytes, sevenZFileInputStream.getBytesRead(), note);
                                        lastDate = currentDate;
                                    }
                                }
                            } catch (javax.xml.stream.XMLStreamException e) {
                                e.printStackTrace();
                            }

                            note = nestingLevel + " " + join("->", nestedZerglings);
                            //printProgress(iTotalBytes, sevenZFileInputStream.getBytesRead(), note);
                        }
                    });
                    myThread.start();

                    try {
                        synchronized (myThread) {
                            myThread.wait();
                        }
                    } catch(InterruptedException ie) {
                    } finally {
                        tmpEntry = sevenZFile.getNextEntry();
                    }

                } catch (javax.xml.stream.XMLStreamException e) {
                    e.printStackTrace();
                }
            }

            sevenZFile.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}
