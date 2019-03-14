package de.retest.recheck.cli.subcommands;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.retest.recheck.cli.PreCondition;
import de.retest.recheck.cli.TestReportUtil;
import de.retest.recheck.persistence.NoStateFileFoundException;
import de.retest.recheck.persistence.Persistence;
import de.retest.recheck.persistence.PersistenceFactory;
import de.retest.recheck.persistence.xml.util.StdXmlClassesProvider;
import de.retest.recheck.report.TestReport;
import de.retest.recheck.suite.flow.ApplyChangesToStatesFlow;
import de.retest.recheck.suite.flow.CreateChangesetForAllDifferencesFlow;
import de.retest.recheck.ui.descriptors.SutState;
import de.retest.recheck.ui.review.ReviewResult;
import de.retest.recheck.ui.review.SuiteChangeSet;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command( name = "commit", description = "Accept given differences." )
public class Commit implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger( Commit.class );

	@Option( names = "--help", usageHelp = true, hidden = true )
	private boolean displayHelp;

	@Option( names = "--all", description = "Accept all differences from the given test report." )
	private boolean all;

	@Parameters( arity = "1",
			description = "Exactly one test report."
					+ " If the test report is not in the project directory, please specify the"
					+ " absolute path, otherwise a relative path is sufficient." )
	private File testReport;

	@Override
	public void run() {
		if ( !PreCondition.isSatisfied() ) {
			return;
		}
		if ( !inputValidation( all, testReport ) ) {
			return;
		}
		try {
			final TestReport report = TestReportUtil.load( testReport );
			if ( !report.containsChanges() ) {
				logger.warn( "The test report has no differences." );
				return;
			}
			TestReportUtil.print( report, testReport );
			final ReviewResult reviewResult = CreateChangesetForAllDifferencesFlow.create( report );
			for ( final SuiteChangeSet suiteChangeSet : reviewResult.getSuiteChangeSets() ) {
				applyChanges( createSutStatePersistence(), suiteChangeSet );
			}
		} catch ( final IOException e ) {
			logger.error( "An error occurred while loading the test report!", e );
		}
	}

	private boolean inputValidation( final boolean all, final File testReport ) {
		if ( !all ) {
			logger.warn( "Currently only the 'commit --all' command is implemented." );
			logger.warn( "A command to commit specific differences will be implemented shortly." );
			return false;
		}
		if ( testReport == null ) {
			logger.error( "Please specify exactly one test report." );
			return false;
		}
		return true;
	}

	private void applyChanges( final Persistence<SutState> persistence, final SuiteChangeSet suiteChangeSet ) {
		try {
			ApplyChangesToStatesFlow.apply( persistence, suiteChangeSet );
		} catch ( final NoStateFileFoundException e ) {
			logger.error( "No state file with name '{}' found!", e.getFilename() );
		}
	}

	private static Persistence<SutState> createSutStatePersistence() {
		return new PersistenceFactory( new HashSet<>( Arrays.asList( StdXmlClassesProvider.getXmlDataClasses() ) ) )
				.getPersistence();
	}

	boolean isDisplayHelp() {
		return displayHelp;
	}

	boolean isAll() {
		return all;
	}

	File getTestReport() {
		return testReport;
	}
}
