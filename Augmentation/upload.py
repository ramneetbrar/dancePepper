import os
path = "../aug_mp4"
files = os.listdir(path)

for f in files:
	if 'boogie' not in f:
			if '.' not in f:
				print('DEBUG:currently adding '+f)
				os.system('git add '+ f)
				os.system('git commit -m \"added '+f+'\" ')
				os.system('git push')
				os.system('git status')

