# er
er for Entity Resolution<br>
This code is an attempt to solve a specific problem of data cleaning in Indian election dataset.<br> 
Almost every political candidate is mentioned more than once, but are not guaranteed to have the same surface form. The task is to identify names that point to the same candidate, but have different surface variations.<br>
This code uses learning techniques to learn how variation are introduced into personal names from original name by reading Wikipedia titles (restricted to only people names) and their redirects. Every variations is encoded as a transformation and all the transformations are scored based on how common they are.<br>
For example, it learns that<br>
F M L -> F L <br>
F M L -> L, F <br>
F L -> Mr. L <br>
are all likely transformations, where F stands for First name, L for Last name, and M for middle name.<br>
It could also learn, word variations such as **Lakshmi -> Laxmi** and replacements like **Robert -> Bob** without any manual intervention.<br>
