failureCode=0
for line in $(find -name '*.nupkg' | grep -G '^((?!\.symbols\.nupkg).)*$'); 
do 
	echo "${NUGET_API_KEY}"
	dotnet nuget push $line -k ${NUGET_API_KEY} -s https://www.nuget.org/api/v2/package
	lastExitCode=$?;
    	echo "lastexitcode=$lastExitCode";
    	if [ $lastExitCode != 0 ] ; then
		echo "setting failurecode $lastExitCode";
    		failureCode=$lastExitCode;
    	fi; 
done;
if [ $failureCode != 0 ] ; then
	echo "exiting with status code $failureCode";
	exit $failureCode;
fi;
for line in $(find -name '*.symbols.nupkg'); 
do 
	echo "${NUGET_API_KEY}"
	dotnet nuget push $line -k ${NUGET_API_KEY} -s https://nuget.smbsrc.net/
	lastExitCode=$?;
    	echo "lastexitcode=$lastExitCode";
    	if [ $lastExitCode != 0 ] ; then
		echo "setting failurecode $lastExitCode";
    		failureCode=$lastExitCode;
    	fi; 
done;
if [ $failureCode != 0 ] ; then
	echo "exiting with status code $failureCode";
	exit $failureCode;
fi;
