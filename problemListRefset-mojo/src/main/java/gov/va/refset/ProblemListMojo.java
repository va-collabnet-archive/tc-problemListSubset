package gov.va.refset;

import gov.va.oia.terminology.converters.sharedUtils.ConsoleUtil;
import gov.va.oia.terminology.converters.sharedUtils.EConceptUtility;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_ContentVersion;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_ContentVersion.BaseContentVersion;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.PropertyType;
import gov.va.oia.terminology.converters.sharedUtils.stats.ConverterUUID;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.dwfa.cement.ArchitectonicAuxiliary;
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
	private String uuidRoot_ = "gov.va.med.term.problemList:";
	private EConceptUtility eConcepts_;
	private ArrayList<PropertyType> propertyTypes_ = new ArrayList<PropertyType>();
	private DataOutputStream dos_;

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
	
	/**
	 * Loader version number
	 * Use parent because project.version pulls in the version of the data file, which I don't want.
	 * 
	 * @parameter expression="${project.parent.version}"
	 * @required
	 */
	private String loaderVersion;

	public void execute() throws MojoExecutionException
	{
		PropertyType contentVersion_ = new BPT_ContentVersion(uuidRoot_);
		contentVersion_.addProperty("Source File Name");
		propertyTypes_.add(contentVersion_);
		
		BufferedReader dataReader = null;
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
			
			dataReader = new BufferedReader(new FileReader(realPath));
			
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
			
			dos_ = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(binaryOutputFile)));
			
			
			eConcepts_ = new EConceptUtility(uuidRoot_);
			
			UUID archRoot = ArchitectonicAuxiliary.Concept.ARCHITECTONIC_ROOT_CONCEPT.getPrimoridalUid();
			UUID metaDataRoot = ConverterUUID.nameUUIDFromBytes((uuidRoot_ + "metadata").getBytes());
			eConcepts_.createAndStoreMetaDataConcept(metaDataRoot, "VA/KP Problem List Metadata", archRoot, dos_);
			eConcepts_.loadMetaDataItems(propertyTypes_, metaDataRoot, dos_);

			EConcept root = eConcepts_.createVARefsetRootConcept();
			root.writeExternal(dos_);
			
			EConcept problemListConcept = eConcepts_.createConcept("VA/KP Problem List", root.getPrimordialUuid());
			eConcepts_.addStringAnnotation(problemListConcept, realPath.getName(), contentVersion_.getProperty("Source File Name").getUUID(), false);
			eConcepts_.addStringAnnotation(problemListConcept, loaderVersion, BaseContentVersion.LOADER_VERSION.getProperty().getUUID(), false);	
			
			for (Problem p :  problemList)
			{
				ConsoleUtil.showProgress();
				UUID memberUuid = Type3UuidFactory.fromSNOMED(p.getSCTID());
				ConverterUUID.addMapping(p.getSCTID(), memberUuid);
				eConcepts_.addRefsetMember(problemListConcept, memberUuid, p.isSubsetActive(), p.getDateOfStatusChange());
			}
			
			problemListConcept.writeExternal(dos_);
			
			dos_.close();
			
			System.out.println("Load Statistics");
			for (String s : eConcepts_.getLoadStats().getSummary())
			{
				System.out.println(s);
			}
			
			//this could be removed from final release.  Just added to help debug editor problems.
			ConsoleUtil.println("Dumping UUID Debug File");
			ConverterUUID.dump(new File(outputDirectory, "problemListUuidDebugMap.txt"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new MojoExecutionException("Failure during export ", e);
		}
		finally
		{
			try
			{
				dataReader.close();
			}
			catch (IOException e)
			{
				//noop
			}
		}
	}
}
