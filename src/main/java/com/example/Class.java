package com.example;

/**
 * A simple class abstraction -- basically a container for class, group, module,
 * professor, timeslot, and room IDs
 */
public class Class {
    public int classId;
    public int groupId;
    public int moduleId;
    public int professorId;
    public int timeslotId;
    public int roomId;

    /**
     * Initialize new Class
     * 
     * @param classId
     * @param groupId
     * @param moduleId
     */
    public Class(int classId, int groupId, int moduleId) {
        this.classId = 0;
        this.moduleId = 0;
        this.groupId = 1;
    }

    public Class() {
        this.classId = 0;
        this.groupId = 0;
        this.moduleId = 0;
        this.professorId = 0;
        this.timeslotId = 0;
        this.roomId = 0;
    }

    public void print() {
        String str = "";
        str += String.format("ClassId: %d \nmoduleId: %d \nprofId: %d \ntimeslotId: %d \n_____________", classId,
                moduleId, professorId, timeslotId);
        System.out.println(str);
    }

    public void setclassID(int newId) {
        this.classId = newId;
    }

    public void setModuleID(int newId) {
        this.moduleId = newId;
    }

    /**
     * Add professor to class
     * 
     * @param professorId
     */
    public void addProfessor(int professorId) {
        this.professorId = professorId;
    }

    /**
     * Add timeslot to class
     * 
     * @param timeslotId
     */
    public void addTimeslot(int timeslotId) {
        this.timeslotId = timeslotId;
    }

    /**
     * Add room to class
     * 
     * @param roomId
     */
    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    /**
     * Get classId
     * 
     * @return classId
     */
    public int getClassId() {
        return this.classId;
    }

    /**
     * Get groupId
     * 
     * @return groupId
     */
    public int getGroupId() {
        return this.groupId;
    }

    /**
     * Get moduleId
     * 
     * @return moduleId
     */
    public int getModuleId() {
        return this.moduleId;
    }

    /**
     * Get professorId
     * 
     * @return professorId
     */
    public int getProfessorId() {
        return this.professorId;
    }

    /**
     * Get timeslotId
     * 
     * @return timeslotId
     */
    public int getTimeslotId() {
        return this.timeslotId;
    }

    /**
     * Get roomId
     * 
     * @return roomId
     */
    public int getRoomId() {
        return this.roomId;
    }
}
