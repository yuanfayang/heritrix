import java.io.IOException;
public abstract class ArcWarcTests 
{
	public static void main(String[] args) 
	{
		try {
			ArcWarc aw = new ArcWarc("warcgz");
			aw.setMode("cdx");
			aw.setFilter("text/html");
			// aw.setOffset();
			aw.setRecordRange(1000,1100);
			aw.printInfo();
			aw.testArc();
		} catch(IOException e) {
			System.err.println("Caught IOException: " 
					+ e.getMessage());
			e.printStackTrace(System.err);
			System.exit(1);
		}

	}
}
