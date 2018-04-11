package com.qualityunit.android.liveagentphone.ui.home;

import java.util.List;

/**
 * Created by rasto on 6.10.16.
 */

public class InternalItem {

    public String id;
    public String number;
    public String status;
    public Agent agent;
    public Department department;

    @Override
    public String toString() {
        return "InternalItem{" +
                "id='" + id + '\'' +
                ", number='" + number + '\'' +
                ", status='" + status + '\'' +
                ", agent=" + agent.toString() +
                ", department=" + department.toString() +
                '}';
    }

    public InternalItem(String id, String number, String status, Agent agent, Department department) {
        this.id = id;
        this.number = number;
        this.status = status;
        this.agent = agent;
        this.department = department;


    }

    public static class Agent {

        public String id;
        public String name;
        public String avatarUrl;

        public Agent(String id, String name, String avatarUrl) {
            this.id = id;
            this.name = name;
            this.avatarUrl = avatarUrl;
        }

        @Override
        public String toString() {
            return "Agent{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", avatarUrl='" + avatarUrl + '\'' +
                    '}';
        }
    }

    public static class Department {

        public String departmentId;
        public String departmentName;
        public List<String> agentIds;

        public Department(String departmentId, String departmentName, List<String> agentIds) {
            this.departmentId = departmentId;
            this.departmentName = departmentName;
            this.agentIds = agentIds;
        }

        @Override
        public String toString() {
            return "Department{" +
                    "departmentId='" + departmentId + '\'' +
                    ", departmentName='" + departmentName + '\'' +
                    ", agentIds=" + agentIds +
                    '}';
        }
    }

}
