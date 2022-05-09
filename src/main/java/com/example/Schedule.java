package com.example;

import java.util.Arrays;

import javax.management.relation.RelationTypeSupport;

public class Schedule {
    public int classID;
    public String crn;
    public String courseTitle;
    public String[] instructors;
    public String[] scheduledTimes;

    public Schedule(int classID, String crn, String courseTitle, String[] instructor, String[] scheduledTime) {
        this.classID = classID;
        this.crn = crn;
        this.courseTitle = courseTitle;
        this.instructors = instructor;
        this.scheduledTimes = scheduledTime;
    }

    public void addInstructorAndTime(String instructor, String timeslot) {
        if (this.instructors != null) {
            int length = instructors.length;
            this.instructors = Arrays.copyOf(this.instructors, length + 1);
            instructors[length] = instructor;
        } else {
            this.instructors = new String[] { instructor };
        }

        if (this.scheduledTimes != null) {
            int length = scheduledTimes.length;
            this.scheduledTimes = Arrays.copyOf(this.scheduledTimes, length + 1);
            scheduledTimes[length] = timeslot;
        } else {
            this.scheduledTimes = new String[] { timeslot };
        }
    }

    private String insTs() {
        String str = "";
        for (int i = 0; i < instructors.length; i++) {
            str += String.format("%s : %s ; ", instructors[i], scheduledTimes[i]);
        }
        return str;
    }

    public int getClassID() {
        return classID;
    }

    public void setClassID(int classID) {
        this.classID = classID;
    }

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public String[] getInstructors() {
        return instructors;
    }

    public void setInstructors(String[] instructor) {
        this.instructors = instructor;
    }

    public String[] getScheduledTimes() {
        return scheduledTimes;
    }

    public void setScheduledTimes(String[] scheduledTime) {
        this.scheduledTimes = scheduledTime;
    }

    public void print() {
        System.out.println(String.format("Class ID: %d", classID));
        System.out.println(String.format("CRN: %s", crn));
        System.out.println(String.format("Course: %s", courseTitle));
        System.out.println(insTs() + "\n");
    }

}
