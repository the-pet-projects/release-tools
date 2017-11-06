failureCode=0
for line in $(find -name '*.nupkg' | grep -P '^((?!\.symbols\.nupkg).)*$'); 
do 
	dotnet nuget push $line -k "${NUGET_API_KEY}" -sk "${NUGET_API_KEY}" -s "https://www.nuget.org/api/v2/package" -ss "https://nuget.smbsrc.net/";
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
