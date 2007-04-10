/**
 *  Provides mutable (and abbreviated) class to aid in the construction of
 *  {@link javax.management.openmbean.OpenMBeanInfo} instances.
 *  
 *  <p>To effectively use this package, you will need to know the following
 *  abbreviations:
 *  
 *  <table border="1">
 *  <tr>
 *  <th>Abbreviation</th>
 *  <th>Antecedent</th>
 *  </tr>
 *  <tr>
 *  <td>Attr</td>
 *  <td>OpenMBeanAttributeInfoSupport</td>
 *  </tr>
 *  <tr>
 *  <td>Cons</td>
 *  <td>OpenMBeanConstructorInfoSupport</td>
 *  </tr>
 *  <tr>
 *  <td>Info</td>
 *  <td>OpenMBeanInfoSupport</td>
 *  </tr>
 *  <tr>
 *  <td>Notif</td>
 *  <td>MBeanNotificationInfo</td>
 *  </tr>
 *  <tr>
 *  <td>Op</td>
 *  <td>OpenMBeanOperationInfoSupport</td>
 *  </tr>
 *  <tr>
 *  <td>Param</td>
 *  <td>OpenMBeanParameterInfoSupport</td>
 *  </tr>
 *  <tr>
 *  <td>def</td>
 *  <td>default value</td>
 *  </tr>
 *  <tr>
 *  <td>desc</td>
 *  <td>description</td>
 *  </tr>
 *  <tr>
 *  <td>max</td>
 *  <td>maximum value</td>
 *  </tr>
 *  <tr>
 *  <td>min</td>
 *  <td>minimum value</td>
 *  </tr>
 *  </table>
 *  
 *  <p>Each class in this package provides a mutable object that can be used
 *  to produce the immutable Info classes required by OpenMBeans.
 */
package org.archive.openmbeans.factory;
