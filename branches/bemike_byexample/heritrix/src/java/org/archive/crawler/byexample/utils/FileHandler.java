package org.archive.crawler.byexample.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;


public class FileHandler {

   /**
    * Create file at path:  filePath/fileName
    * @param filePath
    * @param fileName
    * @return
    * @throws Exception
    */ 
   public static BufferedWriter createFileAtPath (File filePath,String fileName) throws Exception{       
           return new BufferedWriter(new FileWriter(new File(filePath,fileName)));                           
    }
   
   /**
    * Dump StringBuffer to file
    * @param dumpFile
    * @param sb
    * @throws Exception
    */
   public static void dumpBufferToFile(BufferedWriter dumpFile,StringBuffer sb) throws Exception{   
           dumpFile.write(sb.toString());       
   }
   
   public static BufferedReader readBufferFromFile(String filePath) throws Exception{
       return new BufferedReader(new FileReader(filePath));           
   }
   
   /**
    * Close file
    * @param dumpFile
    * @throws Exception
    */
   public static void closeFile(BufferedWriter dumpFile) throws Exception{
       dumpFile.close();
   }
    
    //public static
} //END OF CLASS
