/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.drools.compiler.integrationtests;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.drools.compiler.Alarm;
import org.drools.compiler.Cheese;
import org.drools.compiler.CommonTestMethodBase;
import org.drools.compiler.FactA;
import org.drools.compiler.Foo;
import org.drools.compiler.Pet;
import org.drools.compiler.StockTick;
import org.drools.core.base.UndefinedCalendarExcption;
import org.drools.core.impl.InternalKnowledgeBase;
import org.drools.core.impl.KnowledgeBaseFactory;
import org.drools.core.time.impl.PseudoClockScheduler;
import org.drools.core.util.DateUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.kie.api.KieBase;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.conf.ClockTypeOption;
import org.kie.api.runtime.conf.TimedRuleExecutionOption;
import org.kie.api.runtime.rule.EntryPoint;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.api.time.Calendar;
import org.kie.api.time.SessionClock;
import org.kie.api.time.SessionPseudoClock;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.runtime.StatefulKnowledgeSession;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TimerAndCalendarTest extends CommonTestMethodBase {

    @Test(timeout=15000)
    public void testDuration() throws Exception {
        KieBase kbase = loadKnowledgeBase("test_Duration.drl");
        KieSession ksession = createKnowledgeSession(kbase);
        try {
            final List list = new ArrayList();
            ksession.setGlobal( "list",
                                list );

            final Cheese brie = new Cheese( "brie",
                                            12 );
            final FactHandle brieHandle = ksession.insert(brie );

            ksession.fireAllRules();

            // now check for update
            assertEquals( 0,
                          list.size() );

            // sleep for 500ms
            Thread.sleep( 500 );


            ksession.fireAllRules();
            // now check for update
            assertEquals( 1,
                          list.size() );
        } finally {
            ksession.dispose();
        }
    }

    @Test(timeout=10000)
    public void testDurationWithNoLoop() throws Exception {
        KieBase kbase = loadKnowledgeBase("test_Duration_with_NoLoop.drl");
        KieSession ksession = createKnowledgeSession(kbase);
        try {
            final List list = new ArrayList();
            ksession.setGlobal( "list",
                                list );

            final Cheese brie = new Cheese( "brie",
                                            12 );
            final FactHandle brieHandle = ksession.insert(brie );

            ksession.fireAllRules();

            // now check for update
            assertEquals( 0,
                          list.size() );

            // sleep for 300ms
            Thread.sleep( 300 );

            ksession.fireAllRules();
            // now check for update
            assertEquals( 1,
                          list.size() );
        } finally {
            ksession.dispose();
        }
    }

    @Test(timeout=10000)
    public void testDurationMemoryLeakonRepeatedUpdate() {
        String str = "";
        str += "package org.drools.compiler.test\n";
        str += "import org.drools.compiler.Alarm\n";
        str += "global java.util.List list;";
        str += "rule \"COMPTEUR\"\n";
        str += "  timer (int: 50s)\n";
        str += "  when\n";
        str += "    $alarm : Alarm( number < 5 )\n";
        str += "  then\n";
        str += "    $alarm.incrementNumber();\n";
        str += "    list.add( $alarm );\n";
        str += "    update($alarm);\n";
        str += "end\n";

        KieSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        conf.setOption( ClockTypeOption.get( "pseudo" ) );

        KieBase kbase = loadKnowledgeBaseFromString(str );
        KieSession ksession = createKnowledgeSession(kbase, conf);
        try {
            PseudoClockScheduler timeService = ksession.getSessionClock();
            timeService.advanceTime( new Date().getTime(), TimeUnit.MILLISECONDS );

            List list = new ArrayList();
            ksession.setGlobal( "list",
                                list );
            ksession.insert( new Alarm() );

            ksession.fireAllRules();

            for ( int i = 0; i < 6; i++ ) {
                timeService.advanceTime( 55, TimeUnit.SECONDS );
                ksession.fireAllRules();
            }

            assertEquals(5,
                         list.size() );
        } finally {
            ksession.dispose();
        }
    }
    
    @Test(timeout=10000)
    public void testFireRuleAfterDuration() throws Exception {
        KieBase kbase = loadKnowledgeBase("test_FireRuleAfterDuration.drl");
        KieSession ksession = createKnowledgeSession(kbase);
        try {
            final List list = new ArrayList();
            ksession.setGlobal( "list",
                                list );

            final Cheese brie = new Cheese( "brie",
                                            12 );
            final FactHandle brieHandle = ksession.insert(brie );

            ksession.fireAllRules();

            // now check for update
            assertEquals( 0,
                          list.size() );

            // sleep for 300ms
            Thread.sleep( 300 );

            ksession.fireAllRules();

            // now check for update
            assertEquals( 2,
                          list.size() );
        } finally {
            ksession.dispose();
        }
    }
    
    @Test(timeout=10000)
    public void testNoProtocolIntervalTimer() {
        String str = "";
        str += "package org.simple \n";
        str += "global java.util.List list \n";
        str += "rule xxx \n";
        str += "  duration (30s 10s) ";
        str += "when \n";
        str += "then \n";
        str += "  list.add(\"fired\"); \n";
        str += "end  \n";

        KieSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        conf.setOption( ClockTypeOption.get( "pseudo" ) );

        KieBase kbase = loadKnowledgeBaseFromString(str );
        KieSession ksession = createKnowledgeSession(kbase, conf);
        try {
            List list = new ArrayList();
            PseudoClockScheduler timeService = ksession.getSessionClock();
            timeService.advanceTime( new Date().getTime(), TimeUnit.MILLISECONDS );

            ksession.setGlobal( "list", list );

            ksession.fireAllRules();
            assertEquals( 0, list.size() );

            timeService.advanceTime( 20, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 0, list.size() );

            timeService.advanceTime( 15, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 1, list.size() );

            timeService.advanceTime( 3, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 1, list.size() );

            timeService.advanceTime( 2, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 2, list.size() );

            timeService.advanceTime( 10, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 3, list.size() );
        } finally {
            ksession.dispose();
        }
    }
    
    @Test(timeout=10000)
    public void testIntervalTimer() {
        String str = "";
        str += "package org.simple \n";
        str += "global java.util.List list \n";
        str += "rule xxx \n";
        str += "  timer (int:30s 10s) ";
        str += "when \n";
        str += "then \n";
        str += "  list.add(\"fired\"); \n";
        str += "end  \n";

        KieSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        conf.setOption( ClockTypeOption.get( "pseudo" ) );

        KieBase kbase = loadKnowledgeBaseFromString(str );
        KieSession ksession = createKnowledgeSession(kbase, conf);
        try {
            List list = new ArrayList();

            PseudoClockScheduler timeService = ksession.getSessionClock();
            timeService.advanceTime(new Date().getTime(), TimeUnit.MILLISECONDS);

            ksession.setGlobal("list", list);

            ksession.fireAllRules();
            assertEquals(0, list.size());

            timeService.advanceTime(20, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(0, list.size());

            timeService.advanceTime(15, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(1, list.size());

            timeService.advanceTime(3, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(1, list.size());

            timeService.advanceTime(2, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(2, list.size());

            timeService.advanceTime(10, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertEquals(3, list.size());
        } finally {
            ksession.dispose();
        }
    }

    @Test(timeout=10000)
    public void testIntervalTimerWithoutFire() {
        String str =
                "package org.simple \n" +
                "global java.util.List list \n" +
                "rule xxx \n" +
                "  timer (int:30s 10s) " +
                "when \n" +
                "then \n" +
                "  list.add(\"fired\"); \n" +
                "end  \n";

        KieSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        conf.setOption( ClockTypeOption.get( "pseudo" ) );
        conf.setOption( TimedRuleExecutionOption.YES );

        KieBase kbase = loadKnowledgeBaseFromString(str );
        KieSession ksession = createKnowledgeSession(kbase, conf);
        try {
            List list = new ArrayList();

            PseudoClockScheduler timeService = ksession.getSessionClock();
            timeService.advanceTime( new Date().getTime(), TimeUnit.MILLISECONDS );

            ksession.setGlobal( "list", list );

            ksession.fireAllRules();
            assertEquals( 0, list.size() );

            timeService.advanceTime( 35, TimeUnit.SECONDS );
            assertEquals( 1, list.size() );

            timeService.advanceTime( 10, TimeUnit.SECONDS );
            assertEquals( 2, list.size() );

            timeService.advanceTime( 10, TimeUnit.SECONDS );
            assertEquals( 3, list.size() );
        } finally {
            ksession.dispose();
        }
    }

    @Test(timeout=10000)
    public void testExprIntervalTimerRaceCondition() {
        String str = "";
        str += "package org.simple \n";
        str += "global java.util.List list \n";
        str += "rule xxx \n";
        str += "  timer (expr: $i, $i) \n";
        str += "when \n";
        str += "   $i : Long() \n";
        str += "then \n";
        str += "  list.add(\"fired\"); \n";
        str += "end  \n";

        KieSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        conf.setOption( ClockTypeOption.get( "pseudo" ) );

        KieBase kbase = loadKnowledgeBaseFromString(str );
        KieSession ksession = createKnowledgeSession(kbase, conf);
        try {
            List list = new ArrayList();

            PseudoClockScheduler timeService = ksession.getSessionClock();
            timeService.advanceTime( new Date().getTime(), TimeUnit.MILLISECONDS );

            ksession.setGlobal( "list", list );
            FactHandle fh = ksession.insert(10000l );

            ksession.fireAllRules();
            assertEquals( 0, list.size() );

            timeService.advanceTime( 10, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 1, list.size() );


            timeService.advanceTime( 17, TimeUnit.SECONDS );
            ksession.update( fh, 5000l );
            ksession.fireAllRules();
            assertEquals( 2, list.size() );
        } finally {
            ksession.dispose();
        }
    }

    @Test(timeout=10000)
    public void testUnknownProtocol() {
        wrongTimerExpression("xyz:30");
    }

    @Test(timeout=10000)
    public void testMissingColon() {
        wrongTimerExpression("int 30");
    }

    @Test(timeout=10000)
    public void testMalformedExpression() {
        wrongTimerExpression("30s s30");
    }

    @Test(timeout=10000)
    public void testMalformedIntExpression() {
        wrongTimerExpression("int 30s");
    }

    @Test(timeout=10000)
    public void testMalformedCronExpression() {
        wrongTimerExpression("cron: 0/30 * * * * *");
    }

    private void wrongTimerExpression(String timer) {
        String str = "";
        str += "package org.simple \n";
        str += "rule xxx \n";
        str += "  timer (" + timer + ") ";
        str += "when \n";
        str += "then \n";
        str += "end  \n";

        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        kbuilder.add( ResourceFactory.newByteArrayResource( str.getBytes() ),
                      ResourceType.DRL );

        assertTrue( kbuilder.hasErrors() );
    }

    @Test(timeout=10000)
    public void testCronTimer() throws Exception {
        String str = "";
        str += "package org.simple \n";
        str += "global java.util.List list \n";
        str += "rule xxx \n";
        str += "  timer (cron:15 * * * * ?) ";
        str += "when \n";
        str += "then \n";
        str += "  list.add(\"fired\"); \n";
        str += "end  \n";

        KieSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        conf.setOption( ClockTypeOption.get( "pseudo" ) );

        KieBase kbase = loadKnowledgeBaseFromString(str );
        KieSession ksession = createKnowledgeSession(kbase, conf);
        try {
            List list = new ArrayList();
            PseudoClockScheduler timeService = ksession.getSessionClock();
            DateFormat df = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" );
            Date date = df.parse( "2009-01-01T00:00:00.000-0000" );

            timeService.advanceTime( date.getTime(), TimeUnit.MILLISECONDS );

            ksession.setGlobal( "list", list );

            ksession.fireAllRules();
            assertEquals( 0, list.size() );

            timeService.advanceTime( 10, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 0, list.size() );

            timeService.advanceTime( 10, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 1, list.size() );

            timeService.advanceTime( 30, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 1, list.size() );

            timeService.advanceTime( 30, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 2, list.size() );
        } finally {
            ksession.dispose();
        }
    }
    
    @Test(timeout=10000)
    public void testCalendarNormalRuleSingleCalendar() throws Exception {
        String str = "";
        str += "package org.simple \n";
        str += "global java.util.List list \n";
        str += "rule xxx \n";
        str += "  calendars \"cal1\"\n";
        str += "when \n";
        str += "  String()\n";
        str += "then \n";
        str += "  list.add(\"fired\"); \n";
        str += "end  \n";


        Calendar calFalse = timestamp -> false;
        Calendar calTrue = timestamp -> true;

        KieSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        conf.setOption( ClockTypeOption.get( "pseudo" ) );

        KieBase kbase = loadKnowledgeBaseFromString(str );
        KieSession ksession = createKnowledgeSession(kbase, conf);
        try {
            List list = new ArrayList();
            PseudoClockScheduler timeService = ksession.getSessionClock();
            DateFormat df = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" );
            Date date = df.parse( "2009-01-01T00:00:00.000-0000" );

            ksession.getCalendars().set( "cal1", calTrue );

            timeService.advanceTime( date.getTime(), TimeUnit.MILLISECONDS );
            ksession.setGlobal( "list", list );
            ksession.insert( "o1" );
            ksession.fireAllRules();
            assertEquals( 1, list.size() );

            timeService.advanceTime( 10, TimeUnit.SECONDS );
            ksession.insert( "o2" );
            ksession.fireAllRules();
            assertEquals( 2, list.size() );

            ksession.getCalendars().set( "cal1", calFalse );
            timeService.advanceTime( 10, TimeUnit.SECONDS );
            ksession.insert( "o3" );
            ksession.fireAllRules();
            assertEquals( 2, list.size() );

            ksession.getCalendars().set( "cal1", calTrue );
            timeService.advanceTime( 30, TimeUnit.SECONDS );
            ksession.insert( "o4" );
            ksession.fireAllRules();
            assertEquals( 3, list.size() );
        } finally {
            ksession.dispose();
        }
    }

    @Test
    public void testUndefinedCalendar() {
        String str = "";
        str += "rule xxx \n";
        str += "  calendars \"cal1\"\n";
        str += "when \n";
        str += "then \n";
        str += "end  \n";

        KieBase kbase = loadKnowledgeBaseFromString(str );
        KieSession ksession = createKnowledgeSession(kbase);
        try {
            try {
                ksession.fireAllRules();
                fail("should throw UndefinedCalendarExcption");
            } catch (UndefinedCalendarExcption e) { }
        } finally {
            ksession.dispose();
        }
    }

    @Test(timeout=10000)
    public void testCalendarNormalRuleMultipleCalendars() throws Exception {
        String str = "";
        str += "package org.simple \n";
        str += "global java.util.List list \n";
        str += "rule xxx \n";
        str += "  calendars \"cal1\", \"cal2\"\n";
        str += "when \n";
        str += "  String()\n";
        str += "then \n";
        str += "  list.add(\"fired\"); \n";
        str += "end  \n";

        KieSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        conf.setOption( ClockTypeOption.get( "pseudo" ) );

        KieBase kbase = loadKnowledgeBaseFromString(str );
        KieSession ksession = createKnowledgeSession(kbase, conf);
        try {
            Calendar calFalse = timestamp -> false;

            Calendar calTrue = timestamp -> true;

            List list = new ArrayList();
            PseudoClockScheduler timeService = ksession.getSessionClock();
            DateFormat df = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" );
            Date date = df.parse( "2009-01-01T00:00:00.000-0000" );

            ksession.getCalendars().set( "cal1", calTrue );
            ksession.getCalendars().set( "cal2", calTrue );

            timeService.advanceTime( date.getTime(), TimeUnit.MILLISECONDS );
            ksession.setGlobal( "list", list );
            ksession.insert( "o1" );
            ksession.fireAllRules();
            assertEquals( 1, list.size() );

            ksession.getCalendars().set( "cal2", calFalse );
            timeService.advanceTime( 10, TimeUnit.SECONDS );
            ksession.insert( "o2" );
            ksession.fireAllRules();
            assertEquals( 1, list.size() );

            ksession.getCalendars().set( "cal1", calFalse );
            timeService.advanceTime( 10, TimeUnit.SECONDS );
            ksession.insert( "o3" );
            ksession.fireAllRules();
            assertEquals( 1, list.size() );

            ksession.getCalendars().set( "cal1", calTrue );
            ksession.getCalendars().set( "cal2", calTrue );
            timeService.advanceTime( 30, TimeUnit.SECONDS );
            ksession.insert( "o4" );
            ksession.fireAllRules();
            assertEquals( 2, list.size() );
        } finally {
            ksession.dispose();
        }
    }
    
    @Test(timeout=10000)
    public void testCalendarsWithCron() throws Exception {
        String str = "";
        str += "package org.simple \n";
        str += "global java.util.List list \n";
        str += "rule xxx \n";
        str += "  calendars \"cal1\", \"cal2\"\n";
        str += "  timer (cron:15 * * * * ?) ";
        str += "when \n";
        str += "then \n";
        str += "  list.add(\"fired\"); \n";
        str += "end  \n";

        KieSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        conf.setOption( ClockTypeOption.get( "pseudo" ) );

        KieBase kbase = loadKnowledgeBaseFromString(str );
        KieSession ksession = createKnowledgeSession(kbase, conf);
        try {
            List list = new ArrayList();
            PseudoClockScheduler timeService = ksession.getSessionClock();
            DateFormat df = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" );
            Date date = df.parse( "2009-01-01T00:00:00.000-0000" );

            timeService.advanceTime( date.getTime(), TimeUnit.MILLISECONDS );

            final Date date1 = new Date( date.getTime() +  (15 * 1000) );
            final Date date2 = new Date( date1.getTime() + (60 * 1000) );
            final Date date3 = new Date( date2.getTime() + (60 * 1000) );
            final Date date4 = new Date( date3.getTime() + (60 * 1000) );

            Calendar cal1 = timestamp -> {
                if ( timestamp == date1.getTime() ) {
                    return true;
                } else return timestamp != date4.getTime();
            };

            Calendar cal2 = timestamp -> {
                if ( timestamp == date2.getTime() ) {
                    return false;
                }  else if ( timestamp == date3.getTime() ) {
                    return true;
                } else {
                    return true;
                }
            };

            ksession.getCalendars().set( "cal1", cal1 );
            ksession.getCalendars().set( "cal2", cal2 );

            ksession.setGlobal( "list", list );

            ksession.fireAllRules();
            timeService.advanceTime( 20, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 1, list.size() );

            timeService.advanceTime( 60, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 1, list.size() );

            timeService.advanceTime( 60, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 2, list.size() );

            timeService.advanceTime( 60, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 2, list.size() );

            timeService.advanceTime( 60, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 3, list.size() );

            timeService.advanceTime( 60, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 4, list.size() );
        } finally {
            ksession.dispose();
        }
    }
    
    @Test(timeout=10000)
    public void testCalendarsWithIntervals() throws Exception {
        String str = "";
        str += "package org.simple \n";
        str += "global java.util.List list \n";
        str += "rule xxx \n";
        str += "  calendars \"cal1\", \"cal2\"\n";
        str += "  timer (15s 60s) "; //int: protocol is assumed
        str += "when \n";
        str += "then \n";
        str += "  list.add(\"fired\"); \n";
        str += "end  \n";

        KieSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        conf.setOption( ClockTypeOption.get( "pseudo" ) );

        KieBase kbase = loadKnowledgeBaseFromString(str );
        KieSession ksession = createKnowledgeSession(kbase, conf);
        try {
            List list = new ArrayList();
            PseudoClockScheduler timeService = ksession.getSessionClock();
            DateFormat df = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" );
            Date date = df.parse( "2009-01-01T00:00:00.000-0000" );

            timeService.advanceTime( date.getTime(), TimeUnit.MILLISECONDS );

            final Date date1 = new Date( date.getTime() +  (15 * 1000) );
            final Date date2 = new Date( date1.getTime() + (60 * 1000) );
            final Date date3 = new Date( date2.getTime() + (60 * 1000) );
            final Date date4 = new Date( date3.getTime() + (60 * 1000) );

            Calendar cal1 = timestamp -> {
                if ( timestamp == date1.getTime() ) {
                    return true;
                } else return timestamp != date4.getTime();
            };

            Calendar cal2 = timestamp -> {
                if ( timestamp == date2.getTime() ) {
                    return false;
                }  else if ( timestamp == date3.getTime() ) {
                    return true;
                } else {
                    return true;
                }
            };

            ksession.getCalendars().set( "cal1", cal1 );
            ksession.getCalendars().set( "cal2", cal2 );

            ksession.setGlobal( "list", list );

            ksession.fireAllRules();
            timeService.advanceTime( 20, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 1, list.size() );

            timeService.advanceTime( 60, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 1, list.size() );

            timeService.advanceTime( 60, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 2, list.size() );

            timeService.advanceTime( 60, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 2, list.size() );

            timeService.advanceTime( 60, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 3, list.size() );

            timeService.advanceTime( 60, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 4, list.size() );
        } finally {
            ksession.dispose();
        }
    }
    
    @Test(timeout=10000)
    public void testCalendarsWithIntervalsAndStartAndEnd() throws Exception {
        String str = "";
        str += "package org.simple \n";
        str += "global java.util.List list \n";
        str += "rule xxx \n";
        str += "  calendars \"cal1\"\n";
        str += "  timer (0d 1d; start=3-JAN-2010, end=5-JAN-2010) "; //int: protocol is assumed
        str += "when \n";
        str += "then \n";
        str += "  list.add(\"fired\"); \n";
        str += "end  \n";

        KieSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        conf.setOption( ClockTypeOption.get( "pseudo" ) );

        KieBase kbase = loadKnowledgeBaseFromString(str );
        KieSession ksession = createKnowledgeSession(kbase, conf);
        try {
            List list = new ArrayList();
            PseudoClockScheduler timeService = ksession.getSessionClock();
            DateFormat df = new SimpleDateFormat( "dd-MMM-yyyy", Locale.UK );
            Date date = df.parse( "1-JAN-2010" );

            Calendar cal1 = timestamp -> true;

            long oneDay = 60 * 60 * 24;
            ksession.getCalendars().set( "cal1", cal1 );
            ksession.setGlobal( "list", list );

            timeService.advanceTime( date.getTime(), TimeUnit.MILLISECONDS );
            ksession.fireAllRules();
            assertEquals( 0, list.size() );

            timeService.advanceTime( oneDay, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 0, list.size() );

            timeService.advanceTime( oneDay, TimeUnit.SECONDS );  // day 3
            ksession.fireAllRules();
            assertEquals( 1, list.size() );

            timeService.advanceTime( oneDay, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 2, list.size() );

            timeService.advanceTime( oneDay, TimeUnit.SECONDS );   // day 5
            ksession.fireAllRules();
            assertEquals( 3, list.size() );

            timeService.advanceTime( oneDay, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 3, list.size() );
        } finally {
            ksession.dispose();
        }
    }
    
    @Test(timeout=10000)
    public void testCalendarsWithIntervalsAndStartAndLimit() throws Exception {
        String str = "";
        str += "package org.simple \n";
        str += "global java.util.List list \n";
        str += "rule xxx \n";
        str += "  calendars \"cal1\"\n";
        str += "  timer (0d 1d; start=3-JAN-2010, repeat-limit=4) "; //int: protocol is assumed
        str += "when \n";
        str += "then \n";
        str += "  list.add(\"fired\"); \n";
        str += "end  \n";

        KieSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        conf.setOption(ClockTypeOption.get("pseudo"));

        KieBase kbase = loadKnowledgeBaseFromString(str );
        KieSession ksession = createKnowledgeSession(kbase, conf);
        try {
            List list = new ArrayList();
            PseudoClockScheduler timeService = ksession.getSessionClock();
            DateFormat df = new SimpleDateFormat( "dd-MMM-yyyy", Locale.UK );
            Date date = df.parse( "1-JAN-2010" );

            Calendar cal1 = timestamp -> true;

            long oneDay = 60 * 60 * 24;
            ksession.getCalendars().set( "cal1", cal1 );
            ksession.setGlobal( "list", list );

            timeService.advanceTime( date.getTime(), TimeUnit.MILLISECONDS );
            ksession.fireAllRules();
            assertEquals( 0, list.size() );

            timeService.advanceTime( oneDay, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 0, list.size() );

            timeService.advanceTime( oneDay, TimeUnit.SECONDS ); // day 3
            ksession.fireAllRules();
            assertEquals( 1, list.size() );

            timeService.advanceTime( oneDay, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 2, list.size() );

            timeService.advanceTime( oneDay, TimeUnit.SECONDS );   // day 5
            ksession.fireAllRules();
            assertEquals( 3, list.size() );

            timeService.advanceTime( oneDay, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 3, list.size() );
        } finally {
            ksession.dispose();
        }
    }
    
    @Test(timeout=10000)
    public void testCalendarsWithCronAndStartAndEnd() throws Exception {
        Locale defaultLoc = Locale.getDefault();
        try {
            Locale.setDefault( Locale.UK ); // Because of the date strings in the DRL, fixable with JBRULES-3444
            String str =
                "package org.simple \n" +
                "global java.util.List list \n" +
                "rule xxx \n" +
                "  date-effective \"2-JAN-2010\"\n" +
                "  date-expires \"6-JAN-2010\"\n" +
                "  calendars \"cal1\"\n" +
                "  timer (cron: 0 0 0 * * ?) " +
                "when \n" +
                "then \n" +
                "  list.add(\"fired\"); \n" +
                "end  \n";

            KieSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
            conf.setOption(ClockTypeOption.get("pseudo"));

            KieBase kbase = loadKnowledgeBaseFromString(str );
            KieSession ksession = createKnowledgeSession(kbase, conf);
            try {
                List list = new ArrayList();
                PseudoClockScheduler timeService = ksession.getSessionClock();
                DateFormat df = new SimpleDateFormat( "dd-MMM-yyyy", Locale.UK );
                Date date = df.parse( "1-JAN-2010" );

                Calendar cal1 = timestamp -> true;

                long oneDay = 60 * 60 * 24;
                ksession.getCalendars().set( "cal1", cal1 );
                ksession.setGlobal( "list", list );

                timeService.advanceTime( date.getTime(), TimeUnit.MILLISECONDS );
                ksession.fireAllRules();
                assertEquals( 0, list.size() );

                timeService.advanceTime( oneDay, TimeUnit.SECONDS );
                ksession.fireAllRules();
                assertEquals( 0, list.size() );

                timeService.advanceTime( oneDay, TimeUnit.SECONDS ); // day 3
                ksession.fireAllRules();
                assertEquals( 1, list.size() );

                timeService.advanceTime( oneDay, TimeUnit.SECONDS );
                ksession.fireAllRules();
                assertEquals( 2, list.size() );

                timeService.advanceTime( oneDay, TimeUnit.SECONDS );   // day 5
                ksession.fireAllRules();
                assertEquals( 3, list.size() );

                timeService.advanceTime( oneDay, TimeUnit.SECONDS );
                ksession.fireAllRules();
                assertEquals( 3, list.size() );
            } finally {
                ksession.dispose();
            }
        } finally {
            Locale.setDefault( defaultLoc );
        }
    }

    @Test(timeout=10000)
    public void testCalendarsWithCronAndStartAndLimit() throws Exception {
        Locale defaultLoc = Locale.getDefault();
        try {
            Locale.setDefault( Locale.UK ); // Because of the date strings in the DRL, fixable with JBRULES-3444
            String str =
                    "package org.simple \n" +
                    "global java.util.List list \n" +
                    "rule xxx \n" +
                    "  date-effective \"2-JAN-2010\"\n" +
                    "  calendars \"cal1\"\n" +
                    // FIXME: I have to set the repeate-limit to 6 instead of 4 becuase
                    // it is incremented regardless of the effective date
                    "  timer (cron: 0 0 0 * * ?; repeat-limit=6) " +
                    "when \n" +
                    "then \n" +
                    "  list.add(\"fired\"); \n" +
                    "end  \n";

            KieSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
            conf.setOption(ClockTypeOption.get("pseudo"));

            KieBase kbase = loadKnowledgeBaseFromString(str );
            KieSession ksession = createKnowledgeSession(kbase, conf);
            try {
                List list = new ArrayList();
                PseudoClockScheduler timeService = ksession.getSessionClock();
                DateFormat df = new SimpleDateFormat( "dd-MMM-yyyy", Locale.UK );
                Date date = df.parse( "1-JAN-2010" );

                Calendar cal1 = timestamp -> true;

                long oneDay = 60 * 60 * 24;
                ksession.getCalendars().set( "cal1", cal1 );
                ksession.setGlobal( "list", list );

                timeService.advanceTime( date.getTime(), TimeUnit.MILLISECONDS );
                ksession.fireAllRules();
                assertEquals( 0, list.size() );

                timeService.advanceTime( oneDay, TimeUnit.SECONDS );
                ksession.fireAllRules();
                assertEquals( 0, list.size() );

                timeService.advanceTime( oneDay, TimeUnit.SECONDS ); // day 3
                ksession.fireAllRules();
                assertEquals( 1, list.size() );

                timeService.advanceTime( oneDay, TimeUnit.SECONDS );
                ksession.fireAllRules();
                assertEquals( 2, list.size() );

                timeService.advanceTime( oneDay, TimeUnit.SECONDS );   // day 5
                ksession.fireAllRules();
                assertEquals( 3, list.size() );

                timeService.advanceTime( oneDay, TimeUnit.SECONDS );
                ksession.fireAllRules();
                assertEquals( 4, list.size() );
            } finally {
                ksession.dispose();
            }
        } finally {
            Locale.setDefault( defaultLoc );
        }
    }
    
    @Test(timeout=10000)
    public void testTimerWithNot() throws Exception {
        KieBase kbase = loadKnowledgeBase("test_Timer_With_Not.drl");
        KieSession ksession = createKnowledgeSession(kbase);
        try {
            ksession.fireAllRules();
            Thread.sleep( 200 );
            ksession.fireAllRules();
            Thread.sleep( 200 );
            ksession.fireAllRules();
            // now check that rule "wrap A" fired once, creating one B
            assertEquals( 2, ksession.getFactCount() );
        } finally {
            ksession.dispose();
        }
    }

    @Test(timeout=10000)
    public void testHaltWithTimer() throws Exception {
        KieBase kbase = loadKnowledgeBase("test_Halt_With_Timer.drl");
        final KieSession ksession = createKnowledgeSession(kbase);
        new Thread(ksession::fireUntilHalt).start();
        try {
            Thread.sleep( 1000 );
            FactHandle handle = ksession.insert("halt" );
            Thread.sleep( 2000 );

            // now check that rule "halt" fired once, creating one Integer
            assertEquals( 2, ksession.getFactCount() );
            ksession.delete( handle );
        } finally {
            ksession.halt();
            ksession.dispose();
        }
    }

    @Test(timeout=10000)
    public void testTimerRemoval() throws InterruptedException {
        String str = "package org.drools.compiler.test\n" +
                "import " + TimeUnit.class.getName() + "\n" +
                "global java.util.List list \n" +
                "global " + CountDownLatch.class.getName() + " latch\n" +
                "rule TimerRule \n" +
                "   timer (int:100 50) \n" +
                "when \n" +
                "then \n" +
                "        //forces it to pause until main thread is ready\n" +
                "        latch.await(10, TimeUnit.MINUTES); \n" +
                "        list.add(list.size()); \n" +
                " end";

        KieBase kbase = loadKnowledgeBaseFromString(str );
        KieSession ksession = createKnowledgeSession(kbase);
        try {
            CountDownLatch latch = new CountDownLatch(1);
            List list = Collections.synchronizedList( new ArrayList() );
            ksession.setGlobal( "list", list );
            ksession.setGlobal( "latch", latch );

            ksession.fireAllRules();
            Thread.sleep(500); // this makes sure it actually enters a rule
            kbase.removeRule("org.drools.compiler.test", "TimerRule");
            ksession.fireAllRules();
            latch.countDown();
            Thread.sleep(500); // allow the last rule, if we were in the middle of one to actually fire, before clearing
            ksession.fireAllRules();
            list.clear();
            Thread.sleep(500); // now wait to see if any more fire, they shouldn't
            ksession.fireAllRules();
            assertEquals( 0, list.size() );
        } finally {
            ksession.dispose();
        }
    }

    @Test(timeout=10000)
    public void testIntervalTimerWithLongExpressions() {
        String str = "package org.simple;\n" +
                "global java.util.List list;\n" +
                "\n" +
                "declare Bean\n" +
                "  delay   : long = 30000\n" +
                "  period  : long = 10000\n" +
                "end\n" +

                "\n" +
                "rule init \n" +
                "when \n" +
                "then \n" +
                " insert( new Bean() );\n" +
                "end \n" +
                "\n" +
                "rule xxx\n" +
                "  salience ($d) \n" +
                "  timer( expr: $d, $p; start=3-JAN-2010 )\n" +
                "when\n" +
                "  Bean( $d : delay, $p : period )\n" +
                "then\n" +
                "  list.add( \"fired\" );\n" +
                "end";

        KieSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        conf.setOption(ClockTypeOption.get("pseudo"));

        KieBase kbase = loadKnowledgeBaseFromString(str );
        KieSession ksession = createKnowledgeSession(kbase, conf);
        try {
            List list = new ArrayList();

            PseudoClockScheduler timeService = ksession.getSessionClock();
            timeService.setStartupTime( DateUtils.parseDate("3-JAN-2010").getTime() );

            ksession.setGlobal( "list", list );

            ksession.fireAllRules();
            assertEquals( 0, list.size() );

            timeService.advanceTime( 20, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 0, list.size() );

            timeService.advanceTime( 15, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 1, list.size() );

            timeService.advanceTime( 3, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 1, list.size() );

            timeService.advanceTime( 2, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 2, list.size() );

            timeService.advanceTime( 10, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 3, list.size() );
        } finally {
            ksession.dispose();
        }
    }


    @Test(timeout=10000)
    public void testIntervalTimerWithStringExpressions() {
        checkIntervalTimerWithStringExpressions(false, "3-JAN-2010");
    }

    @Test(timeout=10000)
    public void testIntervalTimerWithAllExpressions() {
        checkIntervalTimerWithStringExpressions(true, "3-JAN-2010");
    }

    @Test(timeout=10000)
    public void testIntervalTimerWithStringExpressionsAfterStart() {
        checkIntervalTimerWithStringExpressions(false, "3-FEB-2010");
    }

    @Test(timeout=10000)
    public void testIntervalTimerWithAllExpressionsAfterStart() {
        checkIntervalTimerWithStringExpressions(true, "3-FEB-2010");
    }

    private void checkIntervalTimerWithStringExpressions(boolean useExprForStart, String startTime) {
        String str = "package org.simple;\n" +
                "global java.util.List list;\n" +
                "\n" +
                "declare Bean\n" +
                "  delay   : String = \"30s\"\n" +
                "  period  : long = 60000\n" +
                "  start   : String = \"3-JAN-2010\"\n" +
                "end\n" +
                "\n" +
                "rule init \n" +
                "when \n" +
                "then \n" +
                " insert( new Bean() );\n" +
                "end \n" +
                "\n" +
                "rule xxx\n" +
                "  salience ($d) \n" +
                "  timer( expr: $d, $p; start=" + (useExprForStart ? "$s" : "3-JAN-2010") +" )\n" +
                "when\n" +
                "  Bean( $d : delay, $p : period, $s : start )\n" +
                "then\n" +
                "  list.add( \"fired\" );\n" +
                "end";

        KieSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        conf.setOption(ClockTypeOption.get("pseudo"));

        KieBase kbase = loadKnowledgeBaseFromString(str );
        KieSession ksession = createKnowledgeSession(kbase, conf);
        try {
            List list = new ArrayList();

            PseudoClockScheduler timeService = ksession.getSessionClock();
            timeService.setStartupTime( DateUtils.parseDate(startTime).getTime() );

            ksession.setGlobal( "list", list );

            ksession.fireAllRules();
            assertEquals( 0, list.size() );

            timeService.advanceTime( 20, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 0, list.size() );

            timeService.advanceTime( 20, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 1, list.size() );

            timeService.advanceTime( 20, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 1, list.size() );

            timeService.advanceTime( 40, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 2, list.size() );

            timeService.advanceTime( 60, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 3, list.size() );

            // simulate a pause in the use of the engine by advancing the system clock
            timeService.setStartupTime(DateUtils.parseDate("3-MAR-2010").getTime());
            list.clear();

            timeService.advanceTime( 20, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 1, list.size() ); // fires once to recover from missing activation

            timeService.advanceTime( 20, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 2, list.size() );

            timeService.advanceTime( 20, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 2, list.size() );

            timeService.advanceTime( 40, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 3, list.size() );

            timeService.advanceTime( 60, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 4, list.size() );
        } finally {
            ksession.dispose();
        }
    }

    @Test(timeout=10000)
    public void testIntervalTimerExpressionWithOr() {
        String text = "package org.kie.test\n"
                      + "global java.util.List list\n"
                      + "import " + FactA.class.getCanonicalName() + "\n"
                      + "import " + Foo.class.getCanonicalName() + "\n"
                      + "import " + Pet.class.getCanonicalName() + "\n"
                      + "rule r1 timer (expr: f1.field2, f1.field2; repeat-limit=3)\n"
                      + "when\n"                      
                      + "    foo: Foo()\n" 
                      + "    ( Pet()  and f1 : FactA( field1 == 'f1') ) or \n"
                      + "    f1 : FactA(field1 == 'f2') \n"                      
                      + "then\n"
                      + "    list.add( f1 );\n"
                      + "    foo.setId( 'xxx' );\n"
                      + "end\n" + "\n";

        KieSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        conf.setOption( ClockTypeOption.get( "pseudo" ) );

        KieBase kbase = loadKnowledgeBaseFromString(text);
        KieSession ksession = createKnowledgeSession(kbase, conf);
        try {
            PseudoClockScheduler timeService = ksession.getSessionClock();
            timeService.advanceTime( new Date().getTime(), TimeUnit.MILLISECONDS );

            List list = new ArrayList();
            ksession.setGlobal( "list", list );
            ksession.insert ( new Foo(null, null) );
            ksession.insert ( new Pet(null) );

            FactA fact1 = new FactA();
            fact1.setField1( "f1" );
            fact1.setField2( 250 );

            FactA fact3 = new FactA();
            fact3.setField1( "f2" );
            fact3.setField2( 1000 );

            ksession.insert( fact1 );
            ksession.insert( fact3 );

            ksession.fireAllRules();
            assertEquals( 0, list.size() );

            timeService.advanceTime( 300, TimeUnit.MILLISECONDS );
            ksession.fireAllRules();
            assertEquals( 1, list.size() );
            assertEquals( fact1, list.get( 0 ) );

            timeService.advanceTime( 300, TimeUnit.MILLISECONDS );
            ksession.fireAllRules();
            assertEquals( 2, list.size() );
            assertEquals( fact1, list.get( 1 ) );

            timeService.advanceTime( 300, TimeUnit.MILLISECONDS );
            ksession.fireAllRules();
            assertEquals( 2, list.size() ); // did not change, repeat-limit kicked in

            timeService.advanceTime( 300, TimeUnit.MILLISECONDS );
            ksession.fireAllRules();
            assertEquals( 3, list.size() );
            assertEquals( fact3, list.get( 2 ) );

            timeService.advanceTime( 1000, TimeUnit.MILLISECONDS );
            ksession.fireAllRules();
            assertEquals( 4, list.size() );
            assertEquals( fact3, list.get( 3 ) );

            timeService.advanceTime( 1000, TimeUnit.MILLISECONDS );
            ksession.fireAllRules();
            assertEquals( 4, list.size() ); // did not change, repeat-limit kicked in
        } finally {
            ksession.dispose();
        }
    }

    @Test(timeout=10000)
    public void testExprTimeRescheduled() {
        String text = "package org.kie.test\n"
                      + "global java.util.List list\n"
                      + "import " + FactA.class.getCanonicalName() + "\n"
                      + "rule r1 timer (expr: f1.field2, f1.field4)\n"
                      + "when\n"                      
                      + "    f1 : FactA() \n"                      
                      + "then\n"
                      + "    list.add( f1 );\n"
                      + "end\n" + "\n";

        KieSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        conf.setOption( ClockTypeOption.get( "pseudo" ) );

        KieBase kbase = loadKnowledgeBaseFromString(text);
        KieSession ksession = createKnowledgeSession(kbase, conf);
        try {
            PseudoClockScheduler timeService = ksession.getSessionClock();
            timeService.advanceTime( new Date().getTime(), TimeUnit.MILLISECONDS );

            List list = new ArrayList();
            ksession.setGlobal( "list", list );

            FactA fact1 = new FactA();
            fact1.setField1( "f1" );
            fact1.setField2( 500 );
            fact1.setField4( 1000 );
            FactHandle fh = ksession.insert (fact1 );

            ksession.fireAllRules();
            assertEquals( 0, list.size() );

            timeService.advanceTime( 1100, TimeUnit.MILLISECONDS );
            ksession.fireAllRules();
            assertEquals( 1, list.size() );
            assertEquals( fact1, list.get( 0 ) );

            timeService.advanceTime( 1100, TimeUnit.MILLISECONDS );
            ksession.fireAllRules();
            assertEquals( 2, list.size() );
            assertEquals( fact1, list.get( 1 ) );

            timeService.advanceTime( 400, TimeUnit.MILLISECONDS );
            ksession.fireAllRules();
            assertEquals( 3, list.size() );
            assertEquals( fact1, list.get( 2 ) );
            list.clear();

            // the activation state of the rule is not changed so the timer isn't reset
            // since the timer alredy fired it will only use only the period that now will be set to 2000
            fact1.setField2( 300 );
            fact1.setField4( 2000 );
            ksession.update(  fh, fact1 );
            ksession.fireAllRules();

            // 100 has passed of the 1000, from the previous schedule
            // so that should be deducted from the 2000 period above, meaning
            //  we only need to increment another 1950
            timeService.advanceTime( 1950, TimeUnit.MILLISECONDS );
            ksession.fireAllRules();
            assertEquals( 1, list.size() );
            assertEquals( fact1, list.get( 0 ) );
            list.clear();

            timeService.advanceTime( 1000, TimeUnit.MILLISECONDS );
            ksession.fireAllRules();
            assertEquals( 0, list.size() );

            timeService.advanceTime( 700, TimeUnit.MILLISECONDS );
            ksession.fireAllRules();
            assertEquals( 0, list.size() );

            timeService.advanceTime( 300, TimeUnit.MILLISECONDS );
            ksession.fireAllRules();
            assertEquals( 1, list.size() );
        } finally {
            ksession.dispose();
        }
    }

    @Test(timeout=10000) @Ignore
    public void testHaltAfterSomeTimeThenRestart() throws Exception {
        String drl = "package org.kie.test;" +
                "global java.util.List list; \n" +
                "\n" +
                "\n" +
                "rule FireAtWill\n" +
                "timer(int:0 100)\n" +
                "when  \n" +
                "then \n" +
                "  list.add( 0 );\n" +
                "end\n" +
                "\n" +
                "rule ImDone\n" +
                "when\n" +
                "  String( this == \"halt\" )\n" +
                "then\n" +
                "  drools.halt();\n" +
                "end\n" +
                "\n" +
                "rule Hi \n" +
                "salience 10 \n" +
                "when \n" +
                "  String( this == \"trigger\" ) \n" +
                "then \n " +
                "  list.add( 5 ); \n" +
                "end \n" +
                "\n" +
                "rule Lo \n" +
                "salience -5 \n" +
                "when \n" +
                "  String( this == \"trigger\" ) \n" +
                "then \n " +
                "  list.add( -5 ); \n" +
                "end \n"
                ;


        KieBase kbase = loadKnowledgeBaseFromString(drl);
        final KieSession ksession = createKnowledgeSession(kbase);
        try {
            List list = new ArrayList();
            ksession.setGlobal( "list", list );

            new Thread(() -> ksession.fireUntilHalt()).start();
            Thread.sleep( 250 );

            assertEquals( asList( 0, 0, 0 ), list );

            ksession.insert( "halt" );
            ksession.insert( "trigger" );
            Thread.sleep( 300 );
            assertEquals( asList( 0, 0, 0 ), list );

            new Thread(() -> ksession.fireUntilHalt()).start();
            Thread.sleep( 200 );

            assertEquals( asList( 0, 0, 0, 5, 0, -5, 0, 0 ), list );
        } finally {
            ksession.halt();
            ksession.dispose();
        }
    }



    @Test (timeout=10000)
    public void testHaltAfterSomeTimeThenRestartButNoLongerHolding() throws Exception {
        String drl = "package org.kie.test;" +
                "global java.util.List list; \n" +
                "\n" +
                "\n" +
                "rule FireAtWill\n" +
                "   timer(int:0 200)\n" +
                "when  \n" +
                "  eval(true)" +
                "  String( this == \"trigger\" )" +
                "then \n" +
                "  list.add( 0 );\n" +
                "end\n" +
                "\n" +
                "rule ImDone\n" +
                "when\n" +
                "  String( this == \"halt\" )\n" +
                "then\n" +
                "  drools.halt();\n" +
                "end\n" +
                "\n"
                ;

        final KieBase kbase = loadKnowledgeBaseFromString(drl);
        final KieSession ksession = createKnowledgeSession(kbase);

        final List list = new ArrayList();
        ksession.setGlobal( "list", list );

        final FactHandle handle = ksession.insert( "trigger" );

        new Thread(ksession::fireUntilHalt).start();
        try {
            Thread.sleep( 350 );
            assertEquals( 2, list.size() ); // delay 0, repeat after 100
            assertEquals( asList( 0, 0 ), list );

            ksession.insert( "halt" );

            Thread.sleep( 200 );
            ksession.delete( handle );
            assertEquals( 2, list.size() ); // halted, no more rule firing

            new Thread(ksession::fireUntilHalt).start();
            try {
                Thread.sleep( 200 );

                assertEquals( 2, list.size() );
                assertEquals( asList( 0, 0 ), list );
            } finally {
                ksession.halt();
            }
        } finally {
            ksession.halt();
            ksession.dispose();
        }
    }

    @Test
    public void testExpiredPropagations() {
        // DROOLS-244
        String drl = "package org.drools.test;\n" +
                     "\n" +
                     "import org.drools.compiler.StockTick;\n" +
                     "global java.util.List list;\n" +
                     "\n" +
                     "declare StockTick\n" +
                     "\t@role( event )\n" +
                     "\t@timestamp( time )\n" +
                     "end\n" +
                     "\n" +
                     "declare window ATicks\n" +
                     " StockTick( company == \"AAA\" ) over window:time( 1s ) " +
                     " from entry-point \"AAA\"\n" +
                     "end\n" +
                     "\n" +
                     "declare window BTicks\n" +
                     " StockTick( company == \"BBB\" ) over window:time( 1s ) " +
                     " from entry-point \"BBB\"\n" +
                     "end\n" +
                     "\n" +
                     "rule Ticks \n" +
                     " when\n" +
                     " String()\n" +
                     " accumulate( $x : StockTick() from window ATicks, $a : count( $x ) )\n" +
                     " accumulate( $y : StockTick() from window BTicks, $b : count( $y ) )\n" +
                     " accumulate( $z : StockTick() over window:time( 1s ), $c : count( $z ) )\n" +
                     " then\n" +
                     " list.add( $a );\n" +
                     " list.add( $b );\n" +
                     " list.add( $c );\n" +
                     "end";


        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        kbuilder.add( ResourceFactory.newByteArrayResource( drl.getBytes() ) , ResourceType.DRL );
        if ( kbuilder.hasErrors() ) { fail( kbuilder.getErrors().toString() ); }

        InternalKnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
        kbase.addPackages(kbuilder.getKnowledgePackages());

        KieSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        conf.setOption( ClockTypeOption.get( "pseudo" ) );
        KieSession ksession = kbase.newKieSession( conf, null );
        try {
            ArrayList list = new ArrayList( );
            ksession.setGlobal( "list", list );

            SessionPseudoClock clock = ksession.getSessionClock();

            clock.advanceTime( 1100, TimeUnit.MILLISECONDS );

            StockTick tick = new StockTick( 0, "AAA", 1.0, 0 );
            StockTick tock = new StockTick( 1, "BBB", 1.0, 2500 );
            StockTick tack = new StockTick( 1, "CCC", 1.0, 2700 );

            EntryPoint epa = ksession.getEntryPoint("AAA");
            EntryPoint epb = ksession.getEntryPoint("BBB");

            epa.insert( tick );
            epb.insert( tock );
            ksession.insert( tack );

            FactHandle handle = ksession.insert("go1");
            ksession.fireAllRules();
            assertEquals( asList( 0L, 1L, 1L ), list );
            list.clear();
            ksession.delete( handle );

            clock.advanceTime( 2550, TimeUnit.MILLISECONDS );

            handle = ksession.insert( "go2" );
            ksession.fireAllRules();
            assertEquals( asList( 0L, 0L, 1L ), list );
            list.clear();
            ksession.delete( handle );

            clock.advanceTime( 500, TimeUnit.MILLISECONDS );

            handle = ksession.insert( "go3" );
            ksession.fireAllRules();
            assertEquals( asList( 0L, 0L, 0L ), list );
            list.clear();
            ksession.delete( handle );
        } finally {
            ksession.dispose();
        }
    }

    @Test
    public void testCronFire() {
        // BZ-1059372
        String drl = "package test.drools\n" +
                     "rule TestRule " +
                     "  timer (cron:* * * * * ?) " +
                     "when\n" +
                     "    String() " +
                     "    Integer() " +
                     "then\n" +
                     "end\n";

        KieBase kbase = loadKnowledgeBaseFromString(drl);
        KieSession ksession = kbase.newKieSession();
        try {
            int repetitions = 10000;
            for (int j = 0; j < repetitions; j++ ) {
                ksession.insert( j );
            }

            ksession.insert( "go" );
            ksession.fireAllRules();
        } finally {
            ksession.dispose();
        }
    }

    @Test(timeout = 10000) @Ignore("the listener callback holds some locks so blocking in it is not safe")
    public void testRaceConditionWithTimedRuleExectionOption() throws Exception {
        // BZ-1073880
        String str = "package org.simple \n" +
                     "global java.util.List list \n" +
                     "rule xxx @Propagation(EAGER)\n" +
                     "  timer (int:30s 10s) "
                     + "when \n" +
                     "  $s: String()\n" +
                     "then \n" +
                     "  list.add($s); \n" +
                     "end  \n";

        KieSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        conf.setOption(ClockTypeOption.get("pseudo"));
        conf.setOption(TimedRuleExecutionOption.YES);

        KieBase kbase = loadKnowledgeBaseFromString(str);
        KieSession ksession = createKnowledgeSession(kbase, conf);
        try {
            final CyclicBarrier barrier = new CyclicBarrier(2);
            final AtomicBoolean aBool = new AtomicBoolean(true);
            AgendaEventListener agendaEventListener = new DefaultAgendaEventListener() {
                public void afterMatchFired(org.kie.api.event.rule.AfterMatchFiredEvent event) {
                    try {
                        if (aBool.get()) {
                            barrier.await();
                            aBool.set(false);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            ksession.addEventListener(agendaEventListener);

            List list = new ArrayList();
            ksession.setGlobal("list", list);

            // Using the Pseudo Clock.
            SessionClock clock = ksession.getSessionClock();
            SessionPseudoClock pseudoClock = (SessionPseudoClock) clock;

            // Insert the event.
            String eventOne = "one";
            ksession.insert(eventOne);

            // Advance the time .... so the timer will fire.
            pseudoClock.advanceTime(10000, TimeUnit.MILLISECONDS);

            // Rule doesn't fire in PHREAK. This is because you need to call 'fireAllRules' after you've inserted the fact, otherwise the timer
            // job is not created.

            ksession.fireAllRules();

            // Rule still doesn't fire, because the DefaultTimerJob is created now, and now we need to advance the timer again.

            pseudoClock.advanceTime(30000, TimeUnit.MILLISECONDS);
            barrier.await();
            barrier.reset();
            aBool.set(true);

            pseudoClock.advanceTime(10000, TimeUnit.MILLISECONDS);
            barrier.await();
            barrier.reset();
            aBool.set(true);

            String eventTwo = "two";
            ksession.insert(eventTwo);
            ksession.fireAllRules();

            // 60
            pseudoClock.advanceTime(10000, TimeUnit.MILLISECONDS);
            barrier.await();
            barrier.reset();
            aBool.set(true);

            // 70
            pseudoClock.advanceTime(10000, TimeUnit.MILLISECONDS);
            barrier.await();
            barrier.reset();
            aBool.set(true);

            //From here, the second rule should fire.
            //phaser.register();
            pseudoClock.advanceTime(10000, TimeUnit.MILLISECONDS);
            barrier.await();
            barrier.reset();
            aBool.set(true);

            // Now 2 rules have fired, and those will now fire every 10 seconds.
            pseudoClock.advanceTime(20000, TimeUnit.MILLISECONDS);
            barrier.await();
            barrier.reset();

            pseudoClock.advanceTime(20000, TimeUnit.MILLISECONDS);
            aBool.set(true);
            barrier.await();
            barrier.reset();

            pseudoClock.advanceTime(20000, TimeUnit.MILLISECONDS);
            aBool.set(true);
            barrier.await();
            barrier.reset();
        } finally {
            ksession.dispose();
        }
    }

    @Test
    public void testSharedTimers() {
        // DROOLS-451
        String str = "package org.simple \n" +
                     "global java.util.List list \n" +
                     "rule R1\n" +
                     "  timer (int:30s 10s) " +
                     "when \n" +
                     "  $i: Integer()\n" +
                     "then \n" +
                     "  System.out.println(\"1\");\n" +
                     "  list.add(\"1\"); \n" +
                     "end  \n" +
                     "rule R2\n" +
                     "  timer (int:30s 10s) " +
                     "when \n" +
                     "  $i: Integer()\n" +
                     "then \n" +
                     "  System.out.println(\"2\");\n" +
                     "  list.add(\"2\"); \n" +
                     "end  \n";

        KieSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        conf.setOption(ClockTypeOption.get("pseudo"));
        conf.setOption(TimedRuleExecutionOption.YES);

        KieBase kbase = loadKnowledgeBaseFromString(str);
        KieSession ksession = createKnowledgeSession(kbase, conf);
        try {
            List<String> list = new ArrayList<String>();
            ksession.setGlobal("list", list);

            SessionClock clock = ksession.getSessionClock();
            SessionPseudoClock pseudoClock = (SessionPseudoClock) clock;

            ksession.insert(1);
            ksession.fireAllRules();
            pseudoClock.advanceTime( 35, TimeUnit.SECONDS );
            ksession.fireAllRules();
            assertEquals( 2, list.size() );
            assertTrue( list.containsAll( asList( "1", "2" ) ) );
        } finally {
            ksession.dispose();
        }
    }

    @Test
    public void testIntervalRuleInsertion() throws Exception {
        // DROOLS-620
        // Does not fail when using pseudo clock due to the subsequent call to fireAllRules
        String str =
                "package org.simple\n" +
                "global java.util.List list\n" +
                "import org.drools.compiler.Alarm\n" +
                "rule \"Interval Alarm\"\n" +
                "timer(int: 1s 1s)\n" +
                "when " +
                "    not Alarm()\n" +
                "then\n" +
                "    insert(new Alarm());\n" +
                "    list.add(\"fired\"); \n" +
                "end\n";

        KieSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        conf.setOption(TimedRuleExecutionOption.YES);
        KieBase kbase = loadKnowledgeBaseFromString(str);
        KieSession ksession = createKnowledgeSession(kbase, conf);
        try {
            List list = new ArrayList();
            ksession.setGlobal( "list", list );

            ksession.fireAllRules();
            assertEquals( 0, list.size() );
            Thread.sleep( 900 );
            assertEquals( 0, list.size() );
            Thread.sleep( 500 );
            assertEquals( 1, list.size() );
        } finally {
            ksession.dispose();
        }
    }
}
