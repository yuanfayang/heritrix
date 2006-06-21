package testing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Test1 {
	public static void main(String[] args) {
		try {
			Process p = Runtime.getRuntime().exec("/opt/swiftfox/swiftfox");
			BufferedReader br = new BufferedReader(new InputStreamReader(
					p.getErrorStream()));
			String l;
			while ((l = br.readLine()) != null) {
				System.out.println(l);
			}
			p.waitFor();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
