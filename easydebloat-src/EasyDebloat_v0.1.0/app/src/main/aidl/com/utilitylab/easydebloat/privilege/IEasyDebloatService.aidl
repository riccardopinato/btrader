package com.utilitylab.easydebloat.privilege;

interface IEasyDebloatService {
    String exec(String command) = 1;
    int uid() = 2;
    void destroy() = 16777114;
}
