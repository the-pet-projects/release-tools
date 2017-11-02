failureCode=0
for line in $(find -name '*.nupkg'); 
do 
	dotnet nuget push $line -k ${NUGET_API_KEY} --symbol-api-key ${NUGET_API_KEY};
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
