package org.archive.crawler.byexample.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.archive.crawler.byexample.constants.OutputConstants;

/**
 * Utilities for reading/writing files 
 * 
 * @author Michael Bendersky
 *
 */
public class FileUtils {

   /**
    * Create file at path:  filePath/fileName
    * @return BufferedWriter to created file
    */ 
   public static BufferedWriter createFileAtPath (String filePath,String fileName){
       
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
      try {
          return new BufferedWriter(new FileWriter(new File(filePath,fileName)));
      } catch (IOException e) {
          throw new RuntimeException("Couldn't create file "+fileName+"at path "+filePath, e );
      }                           
    }
   

   /**
    * Create file at path:  JOBS_HOME/jobNo/filePath/fileName
    * 
    * @param jobNo Job Number
    * @param filePath File path for created file under JOBS_HOME 
    * @param fileName File name for created file
    * @param append if true, then data will be written to the end of the file rather than the beginning
    * @return BufferedWriter to created file
    */
   public static BufferedWriter createFileForJob (String jobNo,String filePath, String fileName, boolean append){
       
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
       try {
           return new BufferedWriter(new FileWriter(new File(filePath,fileName),append));
       } catch (IOException e) {
           throw new RuntimeException("Couldn't create file "+fileName+"at path "+filePath, e );
       }                           
    }
   
   /**
    * Dump StringBuffer to file
    * @param dumpFile BufferedWriter for a file
    * @param sb StringBuffer to dump
    */
   public static void dumpBufferToFile(BufferedWriter dumpFile,StringBuffer sb){   
           try {
               dumpFile.write(sb.toString());  
               dumpFile.flush();
           } catch (IOException e) {
               throw new RuntimeException("Could not dump to file "+dumpFile,e);
           }
   }
   
   /**
    * Read file from filePath to BufferedReader
    * @param filePath path to read file
    * @return BufferedReader with file contents 
    */
   public static BufferedReader readBufferFromFile(String filePath){
       try {
        return new BufferedReader(new FileReader(filePath));
    } catch (FileNotFoundException e) {
        throw new RuntimeException("Could not read from file at path "+filePath,e);
    }           
   }
   
   /**
    * Close dumpFile
    * @param dumpFile BufferedWriter for a file
    */
   public static void closeFile(BufferedWriter dumpFile){
       try {
        dumpFile.close();
    } catch (IOException e) {
        throw new RuntimeException("Could not close file "+dumpFile,e); 
    }
   }
   
} //END OF CLASS
