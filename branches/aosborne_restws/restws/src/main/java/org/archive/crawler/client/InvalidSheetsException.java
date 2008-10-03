package org.archive.crawler.client;

public class InvalidSheetsException extends Exception {
	private static final long serialVersionUID = 2250298185819798710L;
	
	String problems[];
	
	public InvalidSheetsException() {
	}

	public InvalidSheetsException(String message) {
		super(message);
	}

	public InvalidSheetsException(String message, String problems[]) {
		super(message);
		this.problems = problems;
	}
	
	public InvalidSheetsException(Throwable cause) {
		super(cause);
	}

	public InvalidSheetsException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public String[] getProblemSheets() {
		return problems;
	}

}
