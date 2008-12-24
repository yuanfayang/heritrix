import java.io.IOException;
public abstract class ArcWarcTests 
{
	public static void main(String[] args) 
	{
		try {
			if (args.length > 1) {
				int offset    = Integer.valueOf(args[1]); 
				String mode   = (args.length>2) ? args[2] : "scan";
				int start     = (args.length>3) ? Integer.valueOf(args[3]) : 0;
				int end       = (args.length>4) ? Integer.valueOf(args[4]) : 100;
				String filter = (args.length>5) ? args[5] : null;
				ArcWarc aw = new ArcWarc();
				aw.setArcFile(args[0]);
				aw.setParams(null,null,Integer.valueOf(args[1]));
				aw.setArchiveFormat();
				aw.setOffset(offset);
				aw.setMode(mode);
				aw.setRecordRange(start,end);
				aw.setFilter(filter);
				aw.printInfo();
				aw.readArchive();
			} else {
				String usage = "ArcWarcTests.java arc_file offset " 
					+ "[ [count | cdx | index | replay | dump ] " 
					+ "record_range_start record_range_end filter]";
				System.out.println(usage);
			}
		} catch(IOException e) {
			System.err.println("Caught IOException: " 
					+ e.getMessage());
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}
}
