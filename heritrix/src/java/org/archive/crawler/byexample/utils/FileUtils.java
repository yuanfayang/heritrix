package org.archive.crawler.byexample.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import org.archive.crawler.byexample.constants.OutputConstants;


public class FileUtils {

   /**
    * Create file at path:  filePath/fileName
    * @return BufferedWriter to created file
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
    * Create file at path:  JOBS_HOME/jobNo/filePath/fileName
    * 
    * @param jobNo Job Number
    * @param filePath File path for created file under JOBS_HOME 
    * @param fileName File name for created file
    * @param append if true, then data will be written to the end of the file rather than the beginning
    * @return
    * @throws Exception
    */
   public static BufferedWriter createFileForJob (String jobNo,String filePath, String fileName, boolean append) throws Exception{
       
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
       
       filePath=OutputConstants.getJobPath(jobNo)+filePath;
       
       path=new File(filePath);
       if (!path.exists())
           path.mkdir();
      
       
       //Create new file under specified path
      return new BufferedWriter(new FileWriter(new File(filePath,fileName),append));                           
    }
   
   /**
    * Dump StringBuffer to file
    * @param dumpFile BufferedWriter for a file
    * @param sb StringBuffer to dump
    * @throws Exception
    */
   public static void dumpBufferToFile(BufferedWriter dumpFile,StringBuffer sb) throws Exception{   
           dumpFile.write(sb.toString());       
   }
   
   /**
    * Read file from filePath to BufferedReader
    * @param filePath path to read file
    * @return BufferedReader with file contents 
    * @throws Exception
    */
   public static BufferedReader readBufferFromFile(String filePath) throws Exception{
       return new BufferedReader(new FileReader(filePath));           
   }
   
   /**
    * Close dumpFile
    * @param dumpFile BufferedWriter for a file
    * @throws Exception
    */
   public static void closeFile(BufferedWriter dumpFile) throws Exception{
       dumpFile.close();
   }
   
} //END OF CLASS
