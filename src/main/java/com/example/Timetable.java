package com.example;

import java.util.HashMap;

public class Timetable {
    private final HashMap<Integer, Professor> professors;
    private final HashMap<Integer, Module> modules;
    private final HashMap<Integer, Timeslot> timeslots;
    private Class classes[];

    /**
     * Initialize new Timetable
     */
    public Timetable() {
        this.professors = new HashMap<Integer, Professor>();
        this.modules = new HashMap<Integer, Module>();
        this.timeslots = new HashMap<Integer, Timeslot>();
    }

    public Timetable(Timetable cloneable) {
        this.professors = cloneable.getProfessors();
        this.modules = cloneable.getModules();
        this.timeslots = cloneable.getTimeslots();
    }

    private HashMap<Integer, Timeslot> getTimeslots() {
        return this.timeslots;
    }

    private HashMap<Integer, Module> getModules() {
        return this.modules;
    }

    public HashMap<Integer, Professor> getProfessors() {
        return this.professors;
    }

    public void setClasses(Class[] classes) {
        this.classes = classes;
    }

    /**
     * Add new professor
     * 
     * @param professorId
     * @param professorName
     */
    public void addProfessor(int professorId, String professorName) {
        this.professors.put(professorId, new Professor(professorId, professorName));
    }

    public void addProfessor(Professor professor) {
        this.professors.put(professor.getProfessorId(), professor);
    }

    public Class[] getClasses() {
        return this.classes;
    }

    /**
     * Add new module
     * 
     * @param moduleId
     * @param moduleCode
     * @param module
     * @param professorIds
     */
    public void addModule(int moduleId, String moduleCode, String module, int professorIds[]) {
        this.modules.put(moduleId, new Module(moduleId, moduleCode, module, professorIds));
    }

    public void addModule(Module newModule) {
        this.modules.put(newModule.getModuleId(), newModule);
    }

    /**
     * Add new group
     * 
     * @param groupId
     * @param groupSize
     * @param moduleIds
     */

    /**
     * Add new timeslot
     * 
     * @param timeslotId
     * @param timeslot
     */
    public void addTimeslot(int timeslotId, String timeslot) {
        this.timeslots.put(timeslotId, new Timeslot(timeslotId, timeslot));
    }

    public void addTimeslot(int timeslotId, String timeslot, int[] m, int[] t, int[] w, int[] th, int[] f) {
        this.timeslots.put(timeslotId, new Timeslot(timeslotId, timeslot, m, t, w, th, f));
    }

    public void addTimeslot(Timeslot ts) {
        this.timeslots.put(ts.getTimeslotId(), ts);
    }

    /**
     * Get professor from professorId
     * 
     * @param professorId
     * @return professor
     */
    public Professor getProfessor(int professorId) {
        return (Professor) this.professors.get(professorId);
    }

    /**
     * Get module from moduleId
     * 
     * @param moduleId
     * @return module
     */
    public Module getModule(int moduleId) {
        return (Module) this.modules.get(moduleId);
    }

    public int[] getModuleIds() {
        Module[] mods = this.modules.values().toArray(new Module[this.modules.size()]);
        int[] temp = new int[this.modules.size()];
        int idx = 0;
        for (Module m : mods) {
            temp[idx] = m.getModuleId();
            idx++;
        }
        return temp;
    }

    public Professor[] getProfessorsAsArray() {
        return (Professor[]) this.professors.values().toArray(new Professor[this.professors.size()]);
    }

    /**
     * Get timeslot by timeslotId
     * 
     * @param timeslotId
     * @return timeslot
     */
    public Timeslot getTimeslot(int timeslotId) {
        return (Timeslot) this.timeslots.get(timeslotId);
    }

    /**
     * Get random timeslotId
     * 
     * @return timeslot
     */
    public Timeslot getRandomTimeslot() {
        Object[] timeslotArray = this.timeslots.values().toArray();
        Timeslot timeslot = (Timeslot) timeslotArray[(int) (timeslotArray.length * Math.random())];
        return timeslot;
    }
}