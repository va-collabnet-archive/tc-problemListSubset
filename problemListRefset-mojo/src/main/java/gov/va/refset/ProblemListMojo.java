package gov.va.refset;

import gov.va.refset.util.ConsoleUtil;
import gov.va.refset.util.EConceptUtility;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.UUID;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.dwfa.ace.refset.ConceptConstants;
import org.dwfa.util.id.Type3UuidFactory;
import org.ihtsdo.etypes.EConcept;

/**
 * Goal to build the va problem list as a refset
 * 
 * @goal buildProblemListRefset
 * 
 * @phase process-sources
 */
public class ProblemListMojo extends AbstractMojo
{

	/**
	 * Where text (tab delimited) representation of the problem list
	 * 
	 * @parameter
	 * @required
	 */
	private File problemListPath;

	/**
	 * Location of the file.
	 * 
	 * @parameter expression="${project.build.directory}"
	 * @required
	 */
	private File outputDirectory;

	public void execute() throws MojoExecutionException
	{
		try
		{
			//The input path might be a specific file, or it might be a directory (which hopefully contains one file)
			File realPath = null;
			if (problemListPath.isFile())
			{
				realPath = problemListPath;
			}
			else
			{
				for (File f : problemListPath.listFiles())
				{
					if (f.isFile() && f.getName().toLowerCase().endsWith(".txt") && f.getName().toLowerCase().startsWith("problemlist"))
					{
						realPath = f;
						break;
					}
				}
			}
			if (realPath == null)
			{
				throw new MojoExecutionException("Could not find a data file to process after looking in " + problemListPath.getAbsolutePath());
			}
			
			ConsoleUtil.println("Reading problem list " + realPath.getAbsolutePath());
			
			BufferedReader dataReader = new BufferedReader(new FileReader(realPath));
			
			//Line one contains the column names
			//Should be SCTID	FullySpecifiedName	DateOfStatusChange	SubsetStatus	SNOMEDStatus
			dataReader.readLine();  //read and throw away.
			ArrayList<Problem> problemList = new ArrayList<Problem>();
			
			String data = dataReader.readLine();
			while (data != null)
			{
				ConsoleUtil.showProgress();
				if (data.length() > 0)
				{
					problemList.add(new Problem(data.split("\\t")));
				}
				data = dataReader.readLine();
			}

			ConsoleUtil.println("Read " + problemList.size() + " items from the problem list");
			ConsoleUtil.println("Building Refset - writing to " + outputDirectory.getAbsolutePath());
			outputDirectory.mkdir();
			
			
			File binaryOutputFile = new File(outputDirectory, "VA-KP-ProblemList.jbin");
			
			DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(binaryOutputFile)));
			
			
			EConceptUtility eConcepts = new EConceptUtility("gov.va.refset");
			
			EConcept root = eConcepts.createConcept(UUID.nameUUIDFromBytes(("gov.va.refset.VA Refset").getBytes()), "VA Refset", System.currentTimeMillis());
			eConcepts.addRelationship(root, ConceptConstants.REFSET.getUuids()[0], null);
			
			root.writeExternal(dos);
			
			EConcept problemListConcept = eConcepts.createConcept(UUID.nameUUIDFromBytes(("gov.va.refset.VA Refset.VA/KP Problem List").getBytes()), 
					"VA/KP Problem List", System.currentTimeMillis());
			eConcepts.addRelationship(problemListConcept, root.getPrimordialUuid(), null);
			
			
			for (Problem p :  problemList)
			{
				ConsoleUtil.showProgress();
				eConcepts.addRefsetMember(problemListConcept, Type3UuidFactory.fromSNOMED(p.getSCTID()), p.isSubsetActive(), p.getDateOfStatusChange());
			}
			
			problemListConcept.writeExternal(dos);
			
			dos.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new MojoExecutionException("Failure during export ", e);
		}
	}
}
