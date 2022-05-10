package com.example;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.print.Doc;

import com.google.gson.Gson;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.internal.connection.Time;

import org.bson.Document;

public class App implements HttpFunction {
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        // public static void main(String[] args) {
        DBconnection.db_errors.deleteMany(new Document());
        Gson gson = new Gson();
        ArrayList<Collision> errors = initTimetable();
        ArrayList<String> errStr = new ArrayList<String>();
        for (Collision c : errors) {
            errStr.add(c.getCollision());
        }
        errStr = removeDuplicates(errStr);

        for (String er : errStr) {
            String json = gson.toJson(new Collision(er));
            Document doc = Document.parse(json);
            DBconnection.db_errors.insertOne(doc);
        }
    }

    public static ArrayList<String> removeDuplicates(ArrayList<String> list) {

        // Create a new ArrayList
        ArrayList<String> newList = new ArrayList<String>();

        // Traverse through the first list
        for (String element : list) {

            // If this element is not present in newList
            // then add it
            if (!newList.contains(element)) {

                newList.add(element);
            }
        }

        // return the new list
        return newList;
    }

    public static ArrayList<Collision> initTimetable() {
        ArrayList<Collision> errors = new ArrayList<Collision>();
        Course[] courses = getCourseArray();
        Professor[] profs = getInstructorArray();
        Schedule[] schedules = getSchedule();
        Module[] modules = modulesFromClasses(courses, profs);
        Timeslot[] timeslots = getTimeslots();

        Timetable tb = new Timetable();

        // add professors to timetable
        int profMax = 0;
        for (Professor p1 : profs) {
            tb.addProfessor(p1);
            profMax += p1.getMaxClasses();
        }

        int numSections = 0;
        for (Course c1 : courses) {
            numSections += c1.getSections();
        }
        if (numSections > profMax) {
            errors.add(new Collision(
                    String.format("There are %d sections and instructor max classes sum to %d", numSections, profMax)));
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

        HashMap<Integer, Integer> profCounts = new HashMap<Integer, Integer>();
        for (int i = 0; i < profs.length; i++) {
            profCounts.put(profs[i].getProfessorId(), 0);
        }
        // check if other class with same class id is at the same time
        for (Class classA : classes) {
            int classAID = classA.getClassId();
            int classATS = classA.getTimeslotId();
            int classAMID = classA.getModuleId();
            for (Class classB : classes) {
                int classBID = classB.getClassId();
                int classBTS = classB.getTimeslotId();
                int classBMID = classB.getModuleId();

                // check if class a and b are the same
                if (classAID != classBID) {
                    // check if both are ref. to same course
                    if (classAMID == classBMID) {
                        // if both ref same course check the timeslots
                        if (classATS == classBTS) {
                            errors.add(new Collision(
                                    String.format("%s has 2 sections with overlaping timeslots: %s and %s",
                                            tb.getModule(classAMID).getModuleCode(),
                                            tb.getTimeslot(classATS).getTimeslot(),
                                            tb.getTimeslot(classBTS).getTimeslot())));
                        }
                    }
                }
            }
        }

        // Checking for overlaps in the classes.
        for (Class classA : classes) {
            // get current prof assigned to check for overlaps
            for (Class classb : classes) {
                if (classb.getProfessorId() == classA.getProfessorId()) {
                    Timeslot ts1 = tb.getTimeslot(classA.getTimeslotId());
                    int ts2Id = classb.getTimeslotId();
                    if (ts1.avoid == null) {
                        break;
                    } else {
                        for (int i = 0; i < ts1.avoid.length; i++) {
                            if (ts1.avoid[i] == ts2Id) {
                                errors.add(new Collision(String.format(
                                        "%s is scheduled to teach 2 classes at overlapping times: %s at %s, and %s at %s",
                                        tb.getProfessor(classA.getProfessorId()).getProfessorName(),
                                        tb.getModule(classA.getModuleId()).getModuleCode(),
                                        tb.getTimeslot(classA.getTimeslotId()).getTimeslot(),
                                        tb.getModule(classb.getModuleId()).getModuleCode(),
                                        tb.getTimeslot(classb.getTimeslotId()).getTimeslot())));
                                break;
                            }
                        }
                    }
                }
            }
            int currentProfId = classA.getProfessorId();
            profCounts.put(currentProfId, profCounts.getOrDefault(currentProfId, 0) + 1);

            for (Class classB : classes) {
                if (classA.getProfessorId() == classB.getProfessorId()
                        && classA.getTimeslotId() == classB.getTimeslotId()
                        && classA.getClassId() != classB.getClassId()) {
                    errors.add(new Collision(String.format(
                            "%s is scheduled to teach 2 classes at same time: %s and %s at %s",
                            tb.getProfessor(classA.getProfessorId()).getProfessorName(),
                            tb.getModule(classA.getModuleId()).getModuleCode(),
                            tb.getModule(classB.getModuleId()).getModuleCode(),
                            tb.getTimeslot(classB.getTimeslotId()).getTimeslot())));
                    break;
                }
            }
        }

        for (int i = 0; i < profs.length; i++) {
            int maxLimit = profs[i].getMaxClasses();
            int currentAssigned = profCounts.get(profs[i].getProfessorId());
            if (currentAssigned > maxLimit) {
                errors.add(new Collision(String.format("%s has a max of %d, but is assigned %d classes",
                        profs[i].getProfessorName(), profs[i].getMaxClasses(), profs[i].getNumAssigned())));
            }

            else if (currentAssigned == 0) {
                errors.add(new Collision(String.format("%s is not assigned any classes", profs[i].getProfessorName())));
            }

        }

        return errors;

    }

    public static Class[] scheduleToClass(Schedule[] schedules, Module[] modules, Professor[] profs,
            Timeslot[] timeslots) {
        int length = 0;
        for (int i = 0; i < schedules.length; i++) {
            length += schedules[i].getInstructors().length;
        }
        System.out.println(length);
        Class[] classes = new Class[length];
        int classID = 0;
        for (int i = 0; i < schedules.length; i++) {
            for (int j = 0; j < schedules[i].getInstructors().length; j++) {
                int mID = getModuleIdfromCT(schedules[i].getCrn(), modules);
                int pID = findProfId(schedules[i].getInstructors()[j], profs);
                int tsID = findTimeslotId(schedules[i].getScheduledTimes()[j], timeslots);
                classes[classID] = new Class();
                classes[classID].classId = classID;
                classes[classID].moduleId = mID;
                classes[classID].addProfessor(pID);
                classes[classID].addTimeslot(tsID);
                classID++;
            }
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
