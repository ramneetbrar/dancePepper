import os
path = "../OG_mp4"
files = os.listdir(path)

print("DEBUG")
#print(files)
for f in files:
	if '.' not in f:
		subfiles = os.listdir(path+'/'+f)
		for sf in subfiles:
		#	print(sf)
			#print("F is: "+f)
			if sf.endswith('.3gp'):	
				#print(sf.removesuffix('.3gp'))
				os.system('ffmpeg -i ' +f+"/"+ sf +" "+ f+"/"+sf.removesuffix('.3gp') +'.gif')
		os.system('mv '+f+'/*.gif ../OG_gif/'+f+"/")

