// args[0] = arc
//     [1] = offset
//     [2] = mode    = [filter,fetch,cdx,dump]
//     [3] = record_range_start
//     [4] = record_range_end
//     [5] = filter  = [text/html, etc.]
import java.io.IOException;
public abstract class ArcWarcTests 
{
	public static void main(String[] args) 
	{
		try {
			if (args.length > 0) {
				ArcWarc aw = new ArcWarc();
				aw.setArcFile(args[0]);
				aw.setParams(null,null,Integer.valueOf(args[1]));
				aw.setArchiveFormat();
				aw.setOffset(Integer.valueOf(args[1]));
				aw.setMode(args[2]);
				if (args.length > 3) { 
					aw.setRecordRange(Integer.valueOf(args[3]),
							Integer.valueOf(args[4]));
				} else {
					aw.setRecordRange(1,100);
				}
				if (args.length > 5) { 
					aw.setFilter(args[5]);
				}
				aw.printInfo();
				aw.testArc();
			} else {
				ArcWarc aw = new ArcWarc();
			    aw.setDefaultParams("arcgz");
				aw.setDefaultArcFile();
				aw.setArchiveFormat();
				aw.setMode("cdx");
				aw.setFilter("text/html");
				aw.setRecordRange(1000,1100);
				aw.printInfo();
				aw.testArc();
			}
		} catch(IOException e) {
			System.err.println("Caught IOException: " 
					+ e.getMessage());
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}
}
