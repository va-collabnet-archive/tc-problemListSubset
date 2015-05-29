Steps to deploy new source content:

	1) Place the source files into the native-source folder
	2) Update the version number as appropriate in pom.xml
	3) Run a command like this to deploy - (VA is a server name that must be defined with credentials in your maven configuration):
		mvn deploy -DaltDeploymentRepository=VA::default::https://mgr.servers.aceworkspace.net/apps/va-archiva/repository/data-files/
		
Note - new source content should not be checked into SVN.  When finished, simply empty the native-source folder.

For problem-list-refset - the loader currently expects a single text file.
	1) - ProblemListSubset-2009-09-21.txt
	
The date doesn't matter - it just needs to start with "problemlist", and end with ".txt".
	
