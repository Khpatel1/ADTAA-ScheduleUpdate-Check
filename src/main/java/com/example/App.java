package com.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.print.Doc;

import com.google.gson.Gson;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.internal.connection.Time;

import org.bson.Document;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) {
        DBconnection.db_errors.deleteMany(new Document());
        Gson gson = new Gson();
        ArrayList<Error> errors = initTimetable();
        for (Error er : errors) {
            String json = gson.toJson(er);
            Document doc = Document.parse(json);
            DBconnection.db_errors.insertOne(doc);
        }
    }

    public static ArrayList<Error> initTimetable() {
        ArrayList<Error> errors = new ArrayList<Error>();
        Course[] courses = getCourseArray();
        Professor[] profs = getInstructorArray();
        Schedule[] schedules = getSchedule();
        Module[] modules = modulesFromClasses(courses, profs);
        Timeslot[] timeslots = getTimeslots();

        Timetable tb = new Timetable();

        // add professors to timetable
        for (Professor p1 : profs) {
            tb.addProfessor(p1);
        }

        // add modules to timetable
        for (Module m1 : modules) {
            tb.addModule(m1);
        }

        // add timeslots
        for (Timeslot ts1 : timeslots) {
            tb.addTimeslot(ts1);
            ts1.printTimeslot();
        }

        for (Schedule s1 : schedules) {
            s1.print();
        }

        Class[] classes = scheduleToClass(schedules, modules, profs, timeslots);

        // Algorith to check if table is valid
        HashMap<Integer, Integer> profCounts = new HashMap<Integer, Integer>();
        for (int i = 0; i < profs.length; i++) {
            profCounts.put(profs[i].getProfessorId(), 0);
        }
        for (Class classA : classes) {
            classA.print();
            // Recount professor assignments
            int current = profCounts.get(classA.getProfessorId());
            current++;
            profCounts.put(classA.getProfessorId(), current);

            // check times for assigned classes to find overlap
            for (Class classb : classes) {
                if (classb.getProfessorId() == classA.getProfessorId() && classA.getClassId() != classb.getClassId()) {
                    Timeslot ts1 = tb.getTimeslot(classA.getTimeslotId());
                    int ts2Id = classb.getTimeslotId();
                    if (ts1.avoid == null && classA.getTimeslotId() != classb.getTimeslotId()) {
                        break;
                    } else {
                        for (int i = 0; i < ts1.avoid.length; i++) {
                            if (ts1.avoid[i] == ts2Id || classA.getTimeslotId() == classb.getTimeslotId()) {
                                String errmsg = (String.format("%s %s, on %s clashes with %s %s, on %s",
                                        tb.getModule(classA.getModuleId()).getModuleCode(),
                                        tb.getModule(classA.getModuleId()).getModuleName(),
                                        tb.getTimeslot(classA.getTimeslotId()).getTimeslot(),
                                        tb.getModule(classb.getModuleId()).getModuleCode(),
                                        tb.getModule(classb.getModuleId()).getModuleName(),
                                        tb.getTimeslot(classb.getTimeslotId()).getTimeslot()));
                                errors.add(new Error(errmsg));
                                break;
                            }
                        }
                    }
                }
            }
        }

        // check if the professor is within limits
        for (int i = 0; i < profs.length; i++) {
            int maxLimit = profs[i].getMaxClasses();
            int currentAssigned = profCounts.get(profs[i].getProfessorId());
            if (currentAssigned > maxLimit) {
                String errmsg = (String.format("%s is assigned for %d and max is %d", profs[i].getProfessorName(),
                        profs[i].getNumAssigned(), profs[i].maxClasses));
                errors.add(new Error(errmsg));
            }
        }
        return errors;
    }

    public static Class[] scheduleToClass(Schedule[] schedules, Module[] modules, Professor[] profs,
            Timeslot[] timeslots) {
        Class[] classes = new Class[schedules.length];
        for (int i = 0; i < schedules.length; i++) {
            classes[i] = new Class();
            classes[i].classId = schedules[i].getClassID();
            classes[i].moduleId = getModuleIdfromCT(schedules[i].getCrn(), modules);
            classes[i].addProfessor(findProfId(schedules[i].getInstructor(), profs));
            classes[i].addTimeslot(findTimeslotId(schedules[i].getScheduledTime(), timeslots));
        }
        return classes;
    }

    public static int findTimeslotId(String ts, Timeslot[] timeslots) {
        for (int i = 0; i < timeslots.length; i++) {
            if (ts.equals(timeslots[i].getTimeslot())) {
                return timeslots[i].getTimeslotId();
            }
        }
        return -1;
    }

    public static int findProfId(String name, Professor[] profs) {
        for (int i = 0; i < profs.length; i++) {
            if (name.equals(profs[i].getProfessorName())) {
                return profs[i].getProfessorId();
            }
        }
        return -1;
    }

    public static int getModuleIdfromCT(String ct, Module[] modules) {
        for (int i = 0; i < modules.length; i++) {
            if (ct.equals(modules[i].getModuleCode())) {
                return i;
            }
        }
        System.out.println(String.format("%s not found in modules", ct));
        return -1;
    }

    public static Module[] modulesFromClasses(Course[] courses, Professor[] profs) {
        Module[] mods = new Module[courses.length];

        for (int i = 0; i < courses.length; i++) {
            mods[i] = new Module(courses[i].getJavaId(), courses[i].getCourseNumber(), courses[i].getCourseTitle(),
                    findOverlap(courses[i], profs));
        }

        return mods;
    }

    public static Course[] getCourseArray() {
        Gson gson = new Gson();
        int numCourses = (int) DBconnection.db_courses.countDocuments();
        FindIterable<Document> courseFI = DBconnection.db_courses.find();
        MongoCursor<Document> courseCursor = courseFI.iterator();

        int courseID = 0;
        Course[] courses = new Course[numCourses];
        while (courseCursor.hasNext()) {
            courses[courseID] = gson.fromJson(courseCursor.next().toJson(), Course.class);
            courses[courseID].setJavaId(courseID);
            courseID++;
        }
        return courses;
    }

    public static Professor[] getInstructorArray() {
        Gson gson = new Gson();
        int numInstructors = (int) DBconnection.db_instructors.countDocuments();
        FindIterable<Document> instructorFI = DBconnection.db_instructors.find();
        MongoCursor<Document> instructorCursor = instructorFI.iterator();

        int instructorID = 0;
        Professor[] professors = new Professor[numInstructors];
        while (instructorCursor.hasNext()) {
            professors[instructorID] = gson.fromJson(instructorCursor.next().toJson(), Professor.class);
            professors[instructorID].setJavaId(instructorID);
            instructorID++;
        }
        return professors;
    }

    public static Schedule[] getSchedule() {
        Gson gson = new Gson();
        int numSchedule = (int) DBconnection.db_schedule.countDocuments();
        FindIterable<Document> scheduleFI = DBconnection.db_schedule.find();
        MongoCursor<Document> scheduleCursor = scheduleFI.iterator();

        int instructorID = 0;
        Schedule[] schedules = new Schedule[numSchedule];
        while (scheduleCursor.hasNext()) {
            schedules[instructorID] = gson.fromJson(scheduleCursor.next().toJson(), Schedule.class);
            instructorID++;
        }
        return schedules;
    }

    public static Timeslot[] getTimeslots() {
        Gson gson = new Gson();
        int numTimeslots = (int) DBconnection.db_timeslots.countDocuments();
        FindIterable<Document> timeslotFI = DBconnection.db_timeslots.find();
        MongoCursor<Document> timeslotCursor = timeslotFI.iterator();

        int timeslotId = 0;
        Timeslot[] timeslots = new Timeslot[numTimeslots];
        while (timeslotCursor.hasNext()) {
            timeslots[timeslotId] = gson.fromJson(timeslotCursor.next().toJson(), Timeslot.class);
            timeslotId++;
        }
        return timeslots;
    }

    public static int[] findOverlap(Course course, Professor[] instructors) {
        int idx = 0;
        int[] large = new int[100];
        for (int i = 0; i < course.getDisciplines().length; i++) {
            for (int j = 0; j < instructors.length; j++) {
                for (int k = 0; k < instructors[j].getDisciplines().length; k++) {
                    if (course.getDisciplines()[i].equals(instructors[j].getDisciplines()[k])) {
                        large[idx] = instructors[j].getProfessorId();
                        idx++;
                    }
                }
            }
        }
        int[] smaller = Arrays.copyOf(large, idx);
        Arrays.sort(smaller);
        return removeDups(smaller);
    }

    /**
     * Remove all id's which are duplicates and 0
     * 
     * @param og
     * 
     * @return cleanArray
     */
    public static int[] removeDups(int[] a) {
        // Hash map which will store the
        // elements which has appeared previously.
        HashMap<Integer, Boolean> mp = new HashMap<Integer, Boolean>();
        int n = a.length;
        int[] temp = new int[n];
        int idx = 0;

        for (int i = 0; i < n; ++i) {

            // Print the element if it is not
            // present there in the hash map
            // and Insert the element in the hash map
            if (mp.get(a[i]) == null) {
                temp[idx] = a[i];
                idx++;
                mp.put(a[i], true);
            }
        }
        Arrays.sort(temp);
        int numZero = 0;
        for (int i = 0; i < temp.length; i++) {
            if (temp[i] == 0) {
                numZero++;
            }
        }
        int[] fin = new int[temp.length - numZero];
        for (int i = 0; i < fin.length; i++) {
            fin[i] = temp[i + numZero];
        }

        return fin;
    }
}
