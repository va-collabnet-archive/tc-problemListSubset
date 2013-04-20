package gov.va.refset;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class Problem {
	
	private String SCTID_;
	private String fullySpecifiedName_;
	private long dateOfStatusChange_;
	private String subsetStatus_;
	private String snomedStatus_;
	
	private static SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
	
	/**
	 * expects the data to be tab delimited, in the order:
	 * SCTID	FullySpecifiedName	DateOfStatusChange	SubsetStatus	SNOMEDStatus
	 * @throws ParseException 
	 */
	public Problem(String[] problemListLine) throws ParseException
	{
		SCTID_ = problemListLine[0];
		fullySpecifiedName_ = problemListLine[1];
		dateOfStatusChange_ = sdf.parse(problemListLine[2]).getTime();
		subsetStatus_ = problemListLine[3];
		snomedStatus_ = problemListLine[4];
	}

	public String getSCTID() 
	{
		return SCTID_;
	}

	public String getFullySpecifiedName() 
	{
		return fullySpecifiedName_;
	}

	public long getDateOfStatusChange() 
	{
		return dateOfStatusChange_;
	}

	public String getSubsetStatus() 
	{
		return subsetStatus_;
	}
	
	public boolean isSubsetActive()
	{
		return subsetStatus_.equalsIgnoreCase("ACTIVE");
	}

	public String getSnomedStatus() 
	{
		return snomedStatus_;
	}
	
	
}
