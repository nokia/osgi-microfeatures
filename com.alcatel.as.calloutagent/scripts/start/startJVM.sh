#!/usr/bin/perl   
use strict;
no strict 'subs';

use Env;
use FileHandle;
use IPC::Open2;

select ((select(STDOUT), $| = 1)[0]);
select ((select(STDERR), $| = 1)[0]);


#
# variables

my $debug = 0x00; # debug script. noDebug: 0x00, debug results: Â0x01, debug verbose: 0x03

my $memoryUnit="m";  # memory unit for the -Xmx and -Xms java parameters

my $memoryDefault=256;  # default memory when no memory size is defined

my $edenMemoryDefault=10;  # default memory when no Eden memory size is defined

my $systemAdminCmd; # full path name to the systemAdmin command


my $CompileThreshold = 5; # default value for compile threshold (Warming UP)

my $calloutAgentDir; # agent log directory

my $minParameters = 9;

my $javaCde;

my $argvSize;
my $componentName;
my $groupInstance;
my $group;
my $instance;

my $ret;

#
# scanned parameters
my ($CalloutinJVMEdenMemorySize, $CalloutinJVMMemorySize, $CalloutinJVMVerboseGC, $CalloutinJVMStrategyGC, $CalloutinJVMExtendedParameters, $CalloutinJVMWarmingUP);

my @proxyApplications;

my $proxyApplicationsTab = {};

#
# Selected values
#
my $edenMemorySize;
my $memorySize;
my $verboseGC;
my $strategyGC;
my $extendedParameters;
my $warmingUP;

my $memorySizeGenerated="";
my $edenMemorySizeGenerated="";
my $verboseGCGenerated="";
my $strategyGCGenerated="";
my $warmingUPGenerated="";

my $generatedDefines="";


#
# get commands - till token "^Command"
# returns the output in a hash list
sub whileLect {
	my ($h,$lab,$end) = @_;
	my $out = [];
	my $c;
	my $line = "";
	my $loop=1;
	while ($loop && read($h,$c,1)) {
		if ($c eq "\n") {
			next unless (length($line));
			print "whileLect line $lab ln:=<$line>\n" if ($debug&0x02);
			$loop = 0,last if ($line eq "Command:");
			push @$out,($line);
			$line = "";
		}
		else {
			$line .= $c;
			$loop = 0,last if ($line eq "Command:");
		}
	}
	return if ($end eq "end");
	unless (length($c) == 1) {
		print "*** Unexpected End of File while reading systemAdmin.sh, Action: exit\n";
		print "*** For more details set \$debug = 0x03 at line 16 in file <INSTALL_DIR>/scripts/start/startJVM.sh and relaunch the agent\n";
		print "*** Last read bytes: <\n",join(",\n",@$out),"\n>\n";
		exit;
	}
	return $out;
}


#
# return the java version
#
sub getJavaVersion ($) {
	my $jv = shift;

	my $res = `$jv -version 2>&1`;
	$res =~ /version "(\d\.\d)\.(.*)"/;
	return ($1, "$1.$2");
}


#
#
# main code
#
#


#
# check parameters, paths 

print("*** Not enough parameters passed to the shell <@ARGV>, Action: exit\n"),exit if (scalar(@ARGV) < $minParameters);

print("*** The variable JAVA_HOME is not defined, Action: exit"),exit   if (!defined($ENV{JAVA_HOME}));

print("*** The variable INSTALL_DIR is not defined, Action: exit"),exit if (!defined($ENV{INSTALL_DIR}));


#
# compose paths
$systemAdminCmd = "$ENV{INSTALL_DIR}/scripts/admin/systemAdmin.sh";
print("*** The file $systemAdminCmd don't exists. Action: exit\n"),exit unless (-x $systemAdminCmd);
print "systemAdminCmd= <$systemAdminCmd>\n" if ($debug&0x01);

$javaCde = "$ENV{JAVA_HOME}/bin/java";
print("*** The file $javaCde don't exists. Action: exit\n"),exit unless (-x $javaCde);
print "javaCde= <$javaCde>\n" if ($debug&0x01);

$calloutAgentDir = "$ENV{INSTALL_DIR}/var/log";


#
# store passed parameters

my $i =1;
shift @ARGV if ($ARGV[0] eq "--"); # suppress if it exists
foreach (@ARGV) {
	print "options $i:<$_>\n" if ($debug&0x01);
	$i++
}


#
# load group and instance name 
$argvSize = scalar @ARGV;
$componentName = $ARGV[ $argvSize - 7];
$groupInstance = $ARGV[ $argvSize - 6];
($group    = $groupInstance) =~ s/__.*$//;
($instance = $groupInstance) =~ s/^.*__//;
print "componentName= <$componentName>\n" if ($debug&0x01);
print "groupInstance= <$groupInstance>\n" if ($debug&0x01);
print "group........= <$group>\n" if ($debug&0x01);
print "instance.....= <$instance>\n" if ($debug&0x01);


#
# create the pipe with the systemAdmin command
$!="";
my $pidSystemAdmin;
eval {
	$pidSystemAdmin = open2(\*Lecteur, \*Redacteur, "$systemAdminCmd");
};
if ($@) {
	print("*** Error while opening the pipe with $systemAdminCmd. Action: exit\n"),exit;
	exit;
}
Redacteur->autoflush(); # ask for flush output in redacteur

whileLect(Lecteur,"lab1"); # empty prologue
#whileLect(Lecteur,"lab1a"); # empty prologue
print "lab1,lab1a after\n" if ($debug&0x02);


#
#
# listProxyComponentProperties to extract callout values
print ("Redacteur listProxyComponentProperties $group\n") if ($debug&0x02);
print Redacteur "listProxyComponentProperties\n";
print Redacteur "$group\n";
print Redacteur "$componentName\n";
$ret = whileLect(Lecteur,"lab2");
print "lab2<<<\n",join("\n",@$ret),">>>lab2\n" if ($debug&0x02);


#
# find CalloutAgent attributes
my $count=0;
foreach (@$ret) {
	($CalloutinJVMEdenMemorySize = $_)     =~ s/^.*= //,$count++ if (/^.*CalloutinJVMEdenMemorySize/);
	($CalloutinJVMMemorySize = $_)         =~ s/^.*= //,$count++ if (/^.*CalloutinJVMMemorySize/);
	($CalloutinJVMVerboseGC = $_)          =~ s/^.*= //,$count++ if (/^.*CalloutinJVMVerboseGC/);
	($CalloutinJVMStrategyGC =$_)          =~ s/^.*= //,$count++ if (/^.*CalloutinJVMStrategyGC/);
	($CalloutinJVMExtendedParameters =$_)  =~ s/^.*= //,$count++ if (/^.*CalloutinJVMExtendedParameters/);
	($CalloutinJVMWarmingUP =$_)           =~ s/^.*= //,$count++ if (/^.*CalloutinJVMWarmingUP/);
	last if ($count >= 6);
}
print "CalloutinJVMEdenMemorySize.......... <$CalloutinJVMEdenMemorySize>\n"     if ($debug&0x01);
print "CalloutinJVMMemorySize.............. <$CalloutinJVMMemorySize>\n"         if ($debug&0x01);
print "CalloutinJVMVerboseGC............... <$CalloutinJVMVerboseGC>\n"          if ($debug&0x01);
print "CalloutinJVMStrategyGC.............. <$CalloutinJVMStrategyGC>\n"         if ($debug&0x01);
print "CalloutinJVMWarmingUP............... <$CalloutinJVMWarmingUP>\n"          if ($debug&0x01);
print "CalloutinJVMExtendedParameters...... <$CalloutinJVMExtendedParameters>\n" if ($debug&0x01);


#
# search Proxy Applications deployed in the group
# use systemAdmin.sh listProxyAppsInGroup command
print ("Redacteur listProxyAppsInGroup $group\n") if ($debug&0x02);
# print "Avant print Redacteur\n";
eval "print Redacteur \"listProxyAppsInGroup\n\"";
if ($@) { print "***\n ERROR while querying systemAdmin.sh\n"; exit }
print Redacteur "$group\n";
print ("Before whileLect lab3\n") if ($debug&0x02);
$ret = whileLect(Lecteur,"lab3");
print "lab3<<<\n",join("\n",@$ret),">>>lab3\n" if ($debug&0x02);

# find proxy Applications in the group
my $found;
foreach (@$ret) {
	$found = 1,next if (/Deployed applications/);
	next unless ($found);
	chomp;
	next if (length($_) == 0);
	$_ =~ s/^ *//;
	push @proxyApplications,($_);
}
print ("ProxyApplications: total=",scalar(@proxyApplications),",",join(",",@proxyApplications),"\n") if ($debug&0x02);


#
# search the properties for each proxyApplication
foreach (@proxyApplications) {
	my $proxyApp = $_;
	my $hash = {};
	$proxyApplicationsTab->{$proxyApp} = $hash;

	#
	# extract the group parameters
	print ("Redacteur listProxyAppGroupProperties $group $proxyApp\n") if ($debug&0x02);
	print Redacteur "listProxyAppGroupProperties\n";   # enter command listProxyAppGroupProperties
	print Redacteur "$group\n";                        # enter group identification
	print Redacteur "$proxyApp\n";                     # enter proxy application
	$ret = whileLect(Lecteur,"lab4");                  # get results
	print "lab4<<<\n",join("\n",@$ret),">>>lab4\n" if ($debug&0x02);

	my $count = 0;
	foreach (@$ret) {
		($hash->{JVMMemorySizeGroup} = $_)         =~ s/^.*= //,$count++ if (/^.*JVMMemorySize/);
		($hash->{JVMEdenMemorySizeGroup} = $_)     =~ s/^.*= //,$count++ if (/^.*JVMEdenMemorySize/);
		($hash->{JVMVerboseGCGroup} = $_)          =~ s/^.*= //,$count++ if (/^.*JVMVerboseGC/);
		($hash->{JVMStrategyGCGroup} =$_)          =~ s/^.*= //,$count++ if (/^.*JVMStrategyGC/);
		($hash->{JVMExtendedParametersGroup} =$_)  =~ s/^.*= //,$count++ if (/^.*JVMExtendedParameters/);
		last if ($count >= 6);
	}
}

if ($debug&0x01) {
	while (my ($key, $val) = each(%$proxyApplicationsTab)) {
		print "ProxyApplication: $key\n";
		print "\tJVMMemorySizeGroup.........: <$val->{JVMMemorySizeGroup}>\n";
		print "\tJVMEdenMemorySizeGroup.....: <$val->{JVMEdenMemorySizeGroup}>\n";
		print "\tJVMVerboseGCGroup..........: <$val->{JVMVerboseGCGroup}>\n";
		print "\tJVMStrategyGCGroup.........: <$val->{JVMStrategyGCGroup}>\n";
		print "\tJVMExtendedParametersGroup.: <$val->{JVMExtendedParametersGroup}>\n";
	}
}


#
# stop the pipe the systemAdmin command
print Redacteur "exit\n";
whileLect(Lecteur,"","end");
close Lecteur;
close Redacteur;
waitpid($pidSystemAdmin, 1);



#
#
# deduce the selected parameters
#


#
# JVMMemorySize
# CalloutinJVMMemorySize supersedes other definitions
$memorySize = $CalloutinJVMMemorySize;
if ($memorySize == 0) { # CalloutinJVMMemorySize notDefined scan proxyApplication to add then
	while (my ($key, $val) = each(%$proxyApplicationsTab)) {
		$memorySize += $val->{JVMMemorySizeGroup};
	}
}	


#
# JVMEdenMemorySize
# CalloutinJVMEdenMemorySize supersedes other definitions
$edenMemorySize = $CalloutinJVMEdenMemorySize;


#
# JVMVerboseGC
# CalloutinJVMVerboseGC supersedes other definitions
$verboseGC = $CalloutinJVMVerboseGC;
if ($verboseGC eq "NotDefined") {
	while (my ($key, $val) = each(%$proxyApplicationsTab)) {
		$verboseGC = $val->{JVMVerboseGCGroup} if ($val->{JVMVerboseGCGroup} eq "Yes");
	}
}


#
# JVMstrategyGC
# CalloutinJVMStrategyGC superses other definitions
$strategyGC = $CalloutinJVMStrategyGC;
if ($strategyGC eq "NotDefined") {
	while (my ($key, $val) = each(%$proxyApplicationsTab)) {
		$strategyGC = $val->{JVMStrategyGCGroup} if ($val->{JVMStrategyGCGroup} ne "NotDefined");
	}
}


#
# JVMWarmingUP
# JVM warming UP with -XX:CompileThreshold
$warmingUP = $CalloutinJVMWarmingUP;

#
# extendedParameters
# All defined parameters are taken in account
$extendedParameters = $CalloutinJVMExtendedParameters;
while (my ($key, $val) = each(%$proxyApplicationsTab)) {
	$extendedParameters .= " $val->{JVMExtendedParametersGroup}";
}


if ($debug&0x01) {
	print "Selected values:\n";
	print "\tmemorySize............: <$memorySize>\n";
	print "\tedenMemorySize........: <$edenMemorySize>\n";
	print "\tVerboseGC.............: <$verboseGC>\n";
	print "\tstrategyGC............: <$strategyGC>\n";
	print "\tJVMWarmingUP..........: <$warmingUP>\n";
	print "\textendedParameters....: <$extendedParameters>\n";
}


#
#
#set the output values to be generated


#
# memorySize
$memorySize = $memoryDefault if ($memorySize == 0);
$memorySizeGenerated = "-Xmx$memorySize$memoryUnit -Xms$memorySize$memoryUnit";
$generatedDefines= "-DjvmConfiguration.JVMmemorySize=\"$memorySize\"";

#
# edenMemorySize
$edenMemorySize = $edenMemoryDefault if ($edenMemorySize == 0);
# -XX:MaxNewSize=8m -XX:NewSize=8m
$edenMemorySizeGenerated = "-XX:MaxNewSize=$edenMemorySize$memoryUnit -XX:NewSize=$edenMemorySize$memoryUnit";
$generatedDefines= "-DjvmConfiguration.JVMedenMemorySize=\"$edenMemorySize\"";


#
#verboseGC
if ($verboseGC eq "Yes") {
	$verboseGCGenerated="-verbosegc -XX:+PrintGCTimeStamps -XX:+PrintGCDetails -Xloggc:\"$calloutAgentDir/$groupInstance/gc.log\""
}
$generatedDefines .= " -DjvmConfiguration.JVMverboseGC=\"$verboseGC\"";


#
#strategyGC
if ($strategyGC eq "NotDefined") {
	$strategyGCGenerated = "";
}
elsif ($strategyGC eq "Default") {
	$strategyGCGenerated = "";
}
elsif ($strategyGC eq "Parallel") {
	$strategyGCGenerated = " -DCHECKRUNTIMEVERSION=1.5 -DflowControl.gc.cms=true -XX:+UseParNewGC -XX:+CMSParallelRemarkEnabled -XX:SurvivorRatio=128 -XX:MaxTenuringThreshold=0 -XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode -XX:+CMSIncrementalPacing -XX:CMSIncrementalDutyCycleMin=10 -XX:CMSIncrementalDutyCycle=10 -XX:-TraceClassUnloading ";

	# check JavaVersion
	if ($strategyGCGenerated =~ /-DCHECKRUNTIMEVERSION=(\d\.\d)/) {
		my $neededVersion = $1;
		$strategyGCGenerated =~ s/-DCHECKRUNTIMEVERSION=(\d\.\d)//; # suppress the placeholder 

		my ($runVersion, $runVersionFull) = getJavaVersion($javaCde);
		if ($runVersion < $neededVersion) {
			print "***\n***The GC parameters assotiated with the 'Parallel' parameter implies that you must have at least this running java version:<$neededVersion>\n";
			print "*** or you are running the following java version:$runVersionFull\n";
			print "*** The 'Parallel parameter' is ignored\n";
			print "*** Recommendation: Upgrade the running java version\n";
			$strategyGCGenerated = "";
		}
	}
}
elsif  ($strategyGC =~ /Custom/) {
	my $file = "$ENV{INSTALL_DIR}/resource/StrategyGC/$strategyGC";
	my $str;
	if (-f $file) {
		open FILE,"< $file";
		while (<FILE>) {
			$str = $_,last if (/^CUSTOM/);
		}
		close FILE;
		chomp $str;
		$str =~ s/^CUSTOM="//;
		$str =~ s/"$//;
		$strategyGCGenerated = $str;
	}
	else {
		print "*** Strategy: $strategyGC. The file $file must exist. Action: no strategy generation.\n";
	}

	# check JavaVersion
	if ($strategyGCGenerated =~ /-DCHECKRUNTIMEVERSION=(\d\.\d)/) {
		my $neededVersion = $1;
		$strategyGCGenerated =~ s/-DCHECKRUNTIMEVERSION=(\d\.\d)//; # suppress the placeholder 

		my ($runVersion, $runVersionFull) = getJavaVersion($javaCde);
		if ($runVersion < $neededVersion) {
			print "***\n***The customFile:<$file> defines that you must have at least the running java version:<$neededVersion>\n";
			print "*** or you are running the following java version:$runVersionFull\n";
			print "*** The customFile:<$file> is ignored\n";
			print "*** Recommendation: Upgrade the running java version\n";
			$strategyGCGenerated = "";
		}
	}

	# check if -Xmx or -Xms parameters are defined in the Custom file (if yes ignore memory size)
	if ($strategyGCGenerated =~ /-Xm[xs]/) {
		print "***\****Memory size parameters are defined in the customFile:<$file> -> This values supersedes the Memory size parameter\n";
		$memorySizeGenerated = "";
	}

	# check if --XX:MaxNewSize= or -XX:NewSize= parameters are defined in the Custom file (if yes ignore eden memory size)
	if (($strategyGCGenerated =~ /-XX:MaxNewSize=/) || ($strategyGC =~ /-XX:NewSize=/)) {
		print "***\****Eden Memory size parameters are defined in the customFile:<$file> -> This values supersedes the Eden Memory size parameter\n";
		$edenMemorySizeGenerated = "";
	}
}
$generatedDefines .= " -DjvmConfiguration.JVMstrategyGC=\"$strategyGC\"";


#
#warmingUP
if ($warmingUP =~ /yes/i) {
	$warmingUPGenerated = "-XX:CompileThreshold=$CompileThreshold";
	# check if CompileThreshold exists in extendedParameters
	$warmingUPGenerated = "" if ($strategyGCGenerated =~ /CompileThreshold/);
	$generatedDefines .= " -DjvmConfiguration.JVMWarmingUP=\"$warmingUP\"";
}


#
#extendedParameters
$generatedDefines .= " -DjvmConfiguration.JVMextendedParameters=\"$extendedParameters\"";


print "Generated values:\n";
print "\tmemorySizeGenerated............: <$memorySizeGenerated>\n";
print "\tedenMemorySizeGenerated........: <$edenMemorySizeGenerated>\n";
print "\tVerboseGCGenerated.............: <$verboseGCGenerated>\n";
print "\tstrategyGCGenerated............: <$strategyGCGenerated>\n";
print "\twarmingUPGenerated.............: <$warmingUPGenerated>\n";
print "\textendedParameters.............: <$extendedParameters>\n";
print "\tgeneratedDefines...............: <$generatedDefines>\n";


#
#prepare the execute command
my $execCmd = "$javaCde -server $memorySizeGenerated $warmingUPGenerated $edenMemorySizeGenerated $verboseGCGenerated $strategyGCGenerated $extendedParameters $generatedDefines ";


while (@ARGV) {
	my $s = shift @ARGV;
	$execCmd .= " '" . $s . "'";
}
print "\texecCmd..........................: <$execCmd>\n";

# launch plugins before starting the callout
my $pluginsCmd = "$ENV{INSTALL_DIR}/scripts/start/launchPlugins.sh";
print "Calling plugins engine $pluginsCmd\n";
system $pluginsCmd;
print "Plugins engine called\n";

exec $execCmd;


