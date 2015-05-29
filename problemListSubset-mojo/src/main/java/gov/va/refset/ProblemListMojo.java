package gov.va.refset;

import gov.va.oia.terminology.converters.sharedUtils.ConsoleUtil;
import gov.va.oia.terminology.converters.sharedUtils.ConverterBaseMojo;
import gov.va.oia.terminology.converters.sharedUtils.EConceptUtility;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_ContentVersion;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.PropertyType;
import gov.va.oia.terminology.converters.sharedUtils.stats.ConverterUUID;
import gov.va.refset.propertyTypes.PT_RefSets;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.UUID;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.dwfa.cement.ArchitectonicAuxiliary;
import org.dwfa.util.id.Type3UuidFactory;
import org.ihtsdo.etypes.EConcept;

/**
 * Goal to build the va problem list as a refset
 */
@Mojo( name = "buildProblemListSubset", defaultPhase = LifecyclePhase.PROCESS_SOURCES )
public class ProblemListMojo extends ConverterBaseMojo
{
	private String problemListNamespaceSeed_ = "gov.va.med.term.problemList";
	private EConceptUtility eConcepts_;
	private ArrayList<PropertyType> propertyTypes_ = new ArrayList<PropertyType>();
	private DataOutputStream dos_;


	@Override
	public void execute() throws MojoExecutionException
	{
		BufferedReader dataReader = null;
		try
		{
			// The input path might be a specific file, or it might be a directory (which hopefully contains one file)
			File realPath = null;
			if (inputFileLocation.isFile())
			{
				realPath = inputFileLocation;
			}
			else
			{
				for (File f : inputFileLocation.listFiles())
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
				throw new MojoExecutionException("Could not find a data file to process after looking in " + inputFileLocation.getAbsolutePath());
			}

			ConsoleUtil.println("Reading problem list " + realPath.getAbsolutePath());

			dataReader = new BufferedReader(new FileReader(realPath));

			// Line one contains the column names
			// Should be SCTID FullySpecifiedName DateOfStatusChange SubsetStatus SNOMEDStatus
			dataReader.readLine();  // read and throw away.
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
			
			SimpleDateFormat parse = new SimpleDateFormat("yyyy.MM.dd");

			dos_ = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(binaryOutputFile)));
			eConcepts_ = new EConceptUtility(problemListNamespaceSeed_, "ProblemList Path", dos_, parse.parse(converterResultVersion.substring(0, 11)).getTime());

			BPT_ContentVersion contentVersion = new BPT_ContentVersion();
			contentVersion.addProperty("Source File Name");
			propertyTypes_.add(contentVersion);
			PT_RefSets refsets = new PT_RefSets();
			propertyTypes_.add(refsets);

			UUID archRoot = ArchitectonicAuxiliary.Concept.ARCHITECTONIC_ROOT_CONCEPT.getPrimoridalUid();
			UUID metaDataRoot = ConverterUUID.createNamespaceUUIDFromString("metadata");
			eConcepts_.createAndStoreMetaDataConcept(metaDataRoot, "VA/KP Problem List Metadata", false, archRoot, dos_);
			eConcepts_.loadMetaDataItems(propertyTypes_, metaDataRoot, dos_);

			EConcept problemListConcept = refsets.getConcept("VA/KP Problem List");
			eConcepts_.addStringAnnotation(problemListConcept, realPath.getName(), contentVersion.getProperty("Source File Name").getUUID(), false);
			eConcepts_.addStringAnnotation(problemListConcept, loaderVersion, contentVersion.LOADER_VERSION.getUUID(), false);
			eConcepts_.addStringAnnotation(problemListConcept, converterResultVersion, contentVersion.RELEASE.getUUID(), false);

			for (Problem p : problemList)
			{
				ConsoleUtil.showProgress();
				UUID memberUuid = Type3UuidFactory.fromSNOMED(p.getSCTID());
				ConverterUUID.addMapping(p.getSCTID(), memberUuid);
				eConcepts_.addRefsetMember(problemListConcept, memberUuid, null, p.isSubsetActive(), p.getDateOfStatusChange());
			}

			eConcepts_.storeRefsetConcepts(refsets, dos_);

			dos_.close();

			ConsoleUtil.println("Load Statistics");
			for (String s : eConcepts_.getLoadStats().getSummary())
			{
				ConsoleUtil.println(s);
			}

			// this could be removed from final release. Just added to help debug editor problems.
			ConsoleUtil.println("Dumping UUID Debug File");
			ConverterUUID.dump(outputDirectory, "problemListUuid");
			ConsoleUtil.writeOutputToFile(new File(outputDirectory, "ConsoleOutput.txt").toPath());
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
				// noop
			}
		}
	}
}
