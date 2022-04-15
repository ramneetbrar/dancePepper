import os
path = "../aug_gif"
files = os.listdir(path)

print("DEBUG")
#print(files)
for f in files:
	if '.' not in f:
		subfiles = os.listdir(path+'/'+f)
		print("DEBUG:==== now on :" + f)
		for sf in subfiles:
		#	print(sf)
			#print("F is: "+f)
			if sf.endswith('.gif'):	
				#print(sf.removesuffix('.gif'))
				print("x", end='')
				#os.system('ffmpeg -i ' +f+"/"+ sf +" "+ f+"/"+sf.removesuffix('.3gp') +'.gif')
				os.system('ffmpeg -i ' +f+"/"+ sf +" "+ f+"/"+sf.removesuffix('.gif') +'.mp4')
		os.system('mv '+f+'/*.mp4 ../aug_mp4/'+f+"/")

