/**

Enables creation of compliant open MBeans based on metadata provided by 
runtime annotations.

<p>For instance, the following annotated class would provide MBean instances
that provided one "sum" operation and one "baz" attribute:

<pre>
class Foo extends Bean {
    
    @Operation(desc="Sums two integers.", impact=ACTION)
    public int sum(
            @Parameter(name="a", desc="The first integer to sum.")
            int a, 
            
            @Parameter(name="b", desc="The second integer to sum.")
            int b) {
        return a + b;
    }
    
    private double baz;
    
    @Attribute(desc="A read/write double attribute.", def="0")
    public double getBaz() {
        return baz;
    }
    
    public void setBaz(double baz) {
        this.baz = baz;
    }
}
</pre>

 */
package org.archive.openmbeans.annotations;


