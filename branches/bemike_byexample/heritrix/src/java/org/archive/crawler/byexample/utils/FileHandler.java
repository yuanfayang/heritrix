package org.archive.crawler.byexample.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import org.archive.crawler.byexample.constants.OutputConstants;


public class FileHandler {

   /**
    * Create file at path:  filePath/fileName
    * @param filePath
    * @param fileName
    * @return
    * @throws Exception
    */ 
   public static BufferedWriter createFileAtPath (String filePath,String fileName) throws Exception{
       
       //Check if BYEXAMPLE_HOME directory exists
       File path=new File(OutputConstants.BYEXAMPLE_HOME);
       if (!path.exists())
           path.mkdir();
       
       //Check if JOBS_HOME exists
       path=new File(OutputConstants.JOBS_HOME);
       if (!path.exists())
           path.mkdir();
       
       //Check if CONF_HOME exists
       path=new File(OutputConstants.CONFIG_HOME);
       if (!path.exists())
           path.mkdir();
       
       //Check if path under the BYEXAMPLE_HOME/JOBS_HOME directory exists
       path=new File(filePath);
       if (!path.exists())
           path.mkdir();
      
       //Create new file under specified path
      return new BufferedWriter(new FileWriter(new File(filePath,fileName)));                           
    }
   
   /**
    * Create file at path:  filePath/jobNo/fileName
    * @param filePath
    * @param fileName
    * @return
    * @throws Exception
    */ 
   public static BufferedWriter createFileAtPath (String jobNo,String filePath, String fileName) throws Exception{
       
       //Check if BYEXAMPLE_HOME directory exists
       File path=new File(OutputConstants.BYEXAMPLE_HOME);
       if (!path.exists())
           path.mkdir();
       
       //Check if JOBS_HOME exists
       path=new File(OutputConstants.JOBS_HOME);
       if (!path.exists())
           path.mkdir();
       
       //Check if CONF_HOME exists
       path=new File(OutputConstants.CONFIG_HOME);
       if (!path.exists())
           path.mkdir();
       
       
       //Check if path under the BYEXAMPLE_HOME/JOBS_HOME directory exists
       path=new File(OutputConstants.JOBS_HOME+jobNo);
       if (!path.exists())
           path.mkdir();
       
       filePath=OutputConstants.JOBS_HOME+jobNo+filePath;
       
       path=new File(filePath);
       if (!path.exists())
           path.mkdir();
      
       
       //Create new file under specified path
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
   
} //END OF CLASS
